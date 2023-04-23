from typing import List
from collections import defaultdict
import torch
from transformers.models.auto import AutoTokenizer
from transformers import LogitsProcessor, LogitsProcessorList
import logging
from enum import Enum
from abc import ABC, abstractmethod

from validator import SqlValidator, ValidationResult

logger = logging.getLogger(__name__)

class ValidationResultInfo:
    def __init__(self, batch_id, top_token, feed_result):
        self.batch_id = batch_id
        self.top_token = top_token
        self.feed_result = feed_result

class PicardLogitsProcessor(LogitsProcessor):
    '''
    This LogitsProcessor is inspired by the one that is used in the Picard project.
    (https://github.com/ServiceNow/picard/blob/main/seq2seq/utils/picard_model_wrapper.py#L204)
    At each step or when an </s> token is found (depends on is_incremental), we get the tokens with the top k probabilities
    and then go through a number of checks to see if these top tokens are valid or invalid. Validity is determined
    by stop words, forced prefixes and the validity of the SQL. Invalid tokens and the tokens outside of the top k
    are removed from consideration, so they have no chance at being selected during generation.

    The validity of the SQL is tricky. A normal SQL validator/parser aren't able to differentiate between a partially
    complete and invalid clauses. We will use a pyparsing for checking the validity of individual pieces of the overall
    SELECT statement.

    Invalid States:
    - If the top token starts a new clause and the clause before it fails validation, then that token is invalid.
    - If the top token is an incorrect operator for a field, then that token is invalid.
    - If the top token uses an incorrect literal for comparing a field, then that token is invalid.
    '''
    def __init__(
            self,
            tokenizer: AutoTokenizer,
            eos_token_id: int,
            filter_value: float = -float("Inf"),
            max_tokens_to_check: int = 1,
            is_incremental: bool = True,
            forced_prefix_ids: List[int] = [],
            stop_words_ids: List[List[int]] = []
    ):
        '''
        Parameters
        ----------
        tokenizer:
            A huggingface tokenizer for decoding ids for processing reasons.
        eos_token_id:
            The expected token id for the end of the stream (i.e. </s>).
        filter_value:
            The probability value that will be used to filter out tokens so that they don't show up in the results.
            By default it's negative infinity, because there probably shouldn't be a lower value than the filtered tokens.
        max_tokens_to_check:
            This is the top k value. This processor checks the tokens with the top k probabilities.
        is_incremental:
            Determines whether the logits processing as done incrementally or only when the </s> token is encountered.
        forced_prefix_ids:
            These are the ids that must show up in the generated results as the prefix. We will force these ids to
            match the generated results and ignore all other processing rules until these ids have been completely forced.
        stop_word_ids:
            These are lists of ids that represent stop words/phrases. These sequences of ids must not show up in the
            generated results.
        '''
        self.tokenizer = tokenizer
        self.eos_token_id = eos_token_id
        self.filter_value = filter_value
        self.max_tokens_to_check = max_tokens_to_check
        self.is_incremental = is_incremental
        self.forced_prefix_ids = forced_prefix_ids
        self.forced_prefix_ids_len = len(self.forced_prefix_ids)
        self.stop_words_ids = stop_words_ids

        # Maps a batch_id to a validator for that batch.
        # This is for keeping track of what clause is currently being processed for each batch.
        self.batch_validators = defaultdict(lambda: SqlValidator(self.tokenizer, self.eos_token_id))

    def _parse_next_token(self, batch_id: int, input_ids: List[int], top_token: int) -> ValidationResult:
        for stop_word_ids in self.stop_words_ids:
            # Checks if the top token is equal to the last token in the stop word list.
            # Ex: Given "group by" as a stop word, we would wait for the top token to equal "by".
            if top_token == stop_word_ids[-1]:
                if len(stop_word_ids) == 1:
                    return ValidationResultInfo(batch_id, top_token, ValidationResult.INVALID)
                # Handles cases where the stop word is composed of a sequence of ids.
                elif stop_word_ids[:-1] == list(input_ids[1-len(stop_word_ids):]):
                    return ValidationResultInfo(batch_id, top_token, ValidationResult.INVALID)

        input_ids_len = len(input_ids)
        # Checks if we're still forcing the prefix and if the provided token doesn't match what is expected.
        if self.forced_prefix_ids_len > input_ids_len and self.forced_prefix_ids[input_ids_len] != top_token:
            return ValidationResultInfo(batch_id, top_token, ValidationResult.INVALID)
        elif input_ids_len <= self.forced_prefix_ids_len:
            # Why validate further if the input is to be forced?
            return ValidationResultInfo(batch_id, top_token, ValidationResult.COMPLETE_VALID)
        else:
            # After the forced prefix, we use a SQL validator to check if the tokens are valid, partially valid or invalid.
            validation_result = self.batch_validators[batch_id].validate_next_token(input_ids, top_token, self.is_incremental)
            return ValidationResultInfo(batch_id, top_token, validation_result)

    def _feed(self, input_ids: List[int], token: int) -> bool:
        result = self._parse_next_token(1, input_ids, token)
        if result.feed_result == ValidationResult.INVALID:
            logger.debug(f"parsing failure: {input_ids + [token]}")
            return False
        elif result.feed_result == ValidationResult.PARTIAL_VALID:
            logger.debug(f"parsing partial: {input_ids + [token]}")
            return True
        elif result.feed_result == ValidationResult.COMPLETE_VALID:
            logger.debug(f"parsing success: {input_ids + [token]}")
            return True
        else:
            # unexpected parsing result
            raise ValueError("unexpected picard parsing result")

    def _check_token(self, input_ids: List[int], token: int) -> bool:
        if self.is_incremental:
            # check at every step
            return self._feed(input_ids=input_ids, token=token)
        else:
            # only check when decoded string is finalized
            if token == self.eos_token_id:
                return self._feed(input_ids=input_ids, token=token)
            else:
                return True

    def _mask(
            self,
            indices_to_remove: torch.Tensor,
            batch_idx: int,
            input_ids_batch: torch.Tensor,
            top_token: torch.Tensor,
    ) -> None:
        is_valid = self._check_token(input_ids=input_ids_batch.tolist(), token=top_token.item())
        if not is_valid:
            indices_to_remove[batch_idx, top_token] = True

    def _mask_top_k(
            self,
            indices_to_remove: torch.Tensor,
            input_id_batches: torch.Tensor,
            top_token_batches: torch.Tensor,
    ) -> None:
        for batch_idx, (input_ids_batch, top_token_batch) in enumerate(zip(input_id_batches, top_token_batches)):
            for top_token in top_token_batch:
                self._mask(
                    indices_to_remove=indices_to_remove,
                    batch_idx=batch_idx,
                    input_ids_batch=input_ids_batch,
                    top_token=top_token,
                )

    def _batch_mask_top_k(
            self,
            indices_to_remove: torch.Tensor,
            input_id_batches: torch.Tensor,
            top_token_batches: torch.Tensor,
    ) -> None:
        results = []
        for batch_id, (input_id_batch, top_token_batch) in enumerate(zip(input_id_batches, top_token_batches)):
            for top_token in top_token_batch:
                result = self._parse_next_token(batch_id, list(input_id_batch), top_token)
                results.append(result)

        for result in results:
            if result.feed_result == ValidationResult.INVALID:
                logger.debug(f"parsing failure: {input_id_batches[result.batch_id].tolist() + [result.top_token]}")
                indices_to_remove[result.batch_id, result.top_token] = True
            elif result.feed_result == ValidationResult.PARTIAL_VALID:
                logger.debug(f"parsing partial: {input_id_batches[result.batch_id].tolist() + [result.top_token]}")
            elif result.feed_result == ValidationResult.COMPLETE_VALID:
                logger.debug(f"parsing success: {input_id_batches[result.batch_id].tolist() + [result.top_token]}")
            else:
                # unexpected parsing result
                raise ValueError("unexpected picard parsing result")

    @torch.no_grad()
    def __call__(self, input_ids: torch.LongTensor, scores: torch.FloatTensor) -> torch.FloatTensor:
        top_k = min(max(1, self.max_tokens_to_check), scores.size(-1))  # Safety check
        top_scores, top_tokens = torch.topk(scores, top_k)
        # Remove all tokens with a probability less than the last token of the top-k
        lowest_top_k_scores = top_scores[..., -1, None]
        del top_scores
        indices_to_remove = scores < lowest_top_k_scores
        del lowest_top_k_scores
        # Do not mask the EOS token because otherwise production can continue indefinitely if all other tokens are masked
        indices_to_remove[:, self.eos_token_id] = False
        # Mask top-k tokens rejected by picard
        if self.is_incremental:
            self._batch_mask_top_k(
                indices_to_remove=indices_to_remove,
                input_id_batches=input_ids,
                top_token_batches=top_tokens,
            )
        else:
            self._mask_top_k(
                indices_to_remove=indices_to_remove,
                input_id_batches=input_ids,
                top_token_batches=top_tokens,
            )
        del top_tokens
        scores = scores.masked_fill(indices_to_remove, self.filter_value)
        del indices_to_remove
        return scores
