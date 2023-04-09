from transformers.models.auto import AutoTokenizer
from enum import Enum
import re

class ValidationResult(Enum):
    INVALID = 1
    PARTIAL_VALID = 2
    COMPLETE_VALID = 3

class ClauseType(Enum):
    SELECT = 1
    FROM = 2
    WHERE = 3
    ORDER_BY = 4

KEYWORD_TO_CLAUSE_TYPE = {'select': ClauseType.SELECT, 'from': ClauseType.FROM, 'where': ClauseType.WHERE, 'order by': ClauseType.ORDER_BY}

class TokenBuffer:
    def __init__(self, tokenizer: AutoTokenizer, eos_token_id: int):
        self.tokenizer = tokenizer
        self.eos_token_id = eos_token_id
        self.buffer = []
        self.text = ""

    def next_token(self, top_token: int) -> bool:
        '''
        Adds the current token to the clause's buffer. When we are sure we've assembled a word, then we move that word
        along with the delimiter to the clause's text field.
        Returns true if a word has been completed and false otherwise.
        '''
        if top_token == self.eos_token_id:
            self.text += self.tokenizer.decode(self.buffer)
            self.buffer.clear()
            return True

        self.buffer.append(top_token)
        # We attempt to decode the buffer after each token so that we can determine if the word has been completed.
        tmp = self.tokenizer.decode(self.buffer)

        # Positive lookahead splits and preserves the delimiter on the left of the split. Spaces and closing parenthesis
        # are indicators that we have finished assembling a complete word.
        tmp = re.split(r'(?<=\s|\))', tmp)
        # If we've hit a delimiter, then we move the buffer's text into the text for this clause.
        if len(tmp) > 1:
            self.text += tmp[0]
            self.buffer = [top_token]
            return True
        else:
            return False

class Validator:
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
        # Ex: If we start a new clause, then we add information about that clause to the stack.
        # If we enter a sub-clause, then we add information about that clause to the stack.
        # If we complete a clause, then we remove that clause from the stack.
        self.buffer_stack = [TokenBuffer(self.tokenizer, self.eos_token_id)]

        # Each major clause is started with an identifying word.
        self.keyword_ids_to_clause_type = {clause_type: tokenizer(keyword).input_ids[:-1] for keyword, clause_type in KEYWORD_TO_CLAUSE_TYPE.items()}

    def validate_next_token(self, top_token: int) -> ValidationResult:
        # If true, we have a new complete word to process.
        if self.buffer_stack[-1].next_token(top_token):
            #print(self.buffer_stack[-1].text)
            #print(self.buffer_stack[-1].buffer)
            return ValidationResult.COMPLETE_VALID
        else:
            return ValidationResult.PARTIAL_VALID
