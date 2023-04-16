import unittest
import os
from transformers.models.auto import AutoConfig, AutoTokenizer, AutoModelForSeq2SeqLM
from validator import SqlValidator, ValidationResult

dir_path = os.path.dirname(os.path.realpath(__file__))

config = AutoConfig.from_pretrained(os.path.join(dir_path, '../../raw-files/t5.1.1.lm100k.large'))
tokenizer = AutoTokenizer.from_pretrained(os.path.join(dir_path, '../../raw-files/t5.1.1.lm100k.large'))

class SqlValidatorTest(unittest.TestCase):

    def test_happy_path(self):
        validator = SqlValidator(tokenizer, config.eos_token_id)
        valid_sql = 'concert_singer | select t2.concert_name from stadium as t1 join concert as t2 on t1.stadium_id = t2.stadium_id where t1.capacity > 100 order by t1.capacity desc limit 1'
        sql_ids = tokenizer(valid_sql).input_ids
        prev_ids = []
        for id in sql_ids:
            # We want all of the results to come back as partially or completely valid!
            self.assertNotEqual(validator.validate_next_token(prev_ids, id), ValidationResult.INVALID)
            prev_ids.append(id)

        # Checks to make sure the text caching is working as expected.
        cached_ids = sql_ids[:validator.ids_in_text_count]
        expected_cached_text = tokenizer.decode(cached_ids, skip_special_tokens = True)
        self.assertEqual(validator.text.strip(), expected_cached_text)

if __name__ == '__main__':
    unittest.main()