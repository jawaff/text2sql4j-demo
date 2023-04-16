import logging
from typing import List
from enum import Enum
import re
from transformers.models.auto import AutoTokenizer
from parser import generated_select_stmt
import random

class ValidationResult(Enum):
    INVALID = 1
    PARTIAL_VALID = 2
    COMPLETE_VALID = 3

def re_rsplit(pattern, text, maxsplit):
    if maxsplit < 1 or not pattern.search(text): # If split is 0 or less, or upon no match
        return [text]                            # Return the string itself as a one-item list
    prev = len(text)                             # Previous match value start position
    cnt = 0                                      # A match counter
    result = []                                  # Output list
    for m in reversed(list(pattern.finditer(text))):
        result.append(text[m.end():prev])        # Append a match to resulting list
        prev = m.start()                         # Set previous match start position
        cnt += 1                                 # Increment counter
        if cnt == maxsplit:                      # Break out of for loop if...
            break                                # ...match count equals max split value
    result.append(text[:prev])                   # Append the text chunk from start
    return list(reversed(result))                      # Return reversed list

class SqlValidator:
    def __init__(self, tokenizer: AutoTokenizer, eos_token_id: int):
        '''
        Parameters
        ----------
        tokenizer:
            A huggingface tokenizer for decoding ids for processing reasons.
        eos_token_id:
            The expected token id for the end of the stream (i.e. </s>).
        '''
        self.tokenizer = tokenizer
        self.eos_token_id = eos_token_id
        self.delimiter_pattern = re.compile(r'(?<=\s|,|\(|\))')

        self.text = ""
        # Keeps track of the number of ids that have been added to the text.
        self.ids_in_text_count = 0

    def validate_text(self, text: str, parse_all: bool = False) -> ValidationResult:
        try:
            parse_tree = generated_select_stmt.parseString(text, parseAll = parse_all)
            return ValidationResult.COMPLETE_VALID if parse_all else ValidationResult.PARTIAL_VALID
        except Exception as e:
            logging.debug(f'Invalid Text: {text}')
            return ValidationResult.INVALID

    def determine_completed_text(self, cur_buffer: List[int]) -> List[str]:
        buffer_text = self.tokenizer.decode(cur_buffer, skip_special_tokens = True)
        # Positive lookahead splits and preserves the delimiter on the left of the split.
        # When we find a delimiter, we are able to assure that the text before it is complete.
        split_buffer_text = re_rsplit(self.delimiter_pattern, buffer_text, 1)
        # The resulting list will preserve delimiters and should at most have two elements.
        return split_buffer_text

    def cache_completed_text(self, input_ids: List[int]):
        '''
        Attempts to find what text in the provided input_ids is complete and caches it.
        This must take in the complete list of input_ids that was passed to the validator
        and those ids must not include the token! We only want to work with the ids of the inputs
        that are set in stone.
        '''
        # Everything in cur_buffer is set in stone. These are the previously selected values for this beam.
        # We should attempt to add these to our cached text if we've reached a delimiter.
        cur_buffer = input_ids[self.ids_in_text_count:]
        split_buffer_text = self.determine_completed_text(cur_buffer)
        if len(split_buffer_text) > 1:
            self.text += split_buffer_text[0]
            # (is empty check)
            if not split_buffer_text[1]:
                self.ids_in_text_count = len(input_ids)
            else:
                # If the second element is not empty, then one of the tokens in input_ids might be incomplete
                # and will not be cached.
                self.ids_in_text_count = len(input_ids) - 1

    def validate_next_token(self, input_ids: List[int], top_token: int, is_incremental=False) -> ValidationResult:
        if top_token == self.eos_token_id:
            text = self.tokenizer.decode(input_ids, skip_special_tokens = True)
            result = self.validate_text(text, parse_all = True)
            return result
        else:
            # Buffer does not include the text we've already decoded completely!
            cur_buffer = input_ids[self.ids_in_text_count:] + [top_token]
            split_buffer_text = self.determine_completed_text(cur_buffer)
            # If we've hit a delimiter, then we are able to potentially parse the text to check validity.
            if len(split_buffer_text) > 1:
                result = self.validate_text(self.text + split_buffer_text[0])
            else:
                result = ValidationResult.PARTIAL_VALID

            # Attempts to cache text we know that is complete in order to reduce redundant decoding.
            self.cache_completed_text(input_ids)

            return result
