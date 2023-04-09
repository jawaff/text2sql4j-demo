import os
import torch
from transformers.models.auto import AutoConfig, AutoTokenizer, AutoModelForSeq2SeqLM
from transformers import LogitsProcessor, LogitsProcessorList
from logits import PicardMode, PicardLogitsProcessor

dir_path = os.path.dirname(os.path.realpath(__file__))

class Translator:
    def __init__(self):
        self.config = AutoConfig.from_pretrained(os.path.join(dir_path, '../raw-files/t5.1.1.lm100k.large'))
        self.tokenizer = AutoTokenizer.from_pretrained(os.path.join(dir_path, '../raw-files/t5.1.1.lm100k.large'))
        self.model = AutoModelForSeq2SeqLM.from_pretrained(os.path.join(dir_path, '../raw-files/t5.1.1.lm100k.large'), config=self.config)

        self.picard_mode = PicardMode.LEXING
        self.max_tokens_to_check = 3

        stop_words = ['group by', 'limit', 'offset']
        # </s> should be excluded from these lists!
        # Each word is converted into a separate list of ids
        self.stop_words_ids = [words_ids[:-1] for words_ids in self.tokenizer(stop_words).input_ids]

    def translate(self, expected_prefix, raw_input):
        # </s> should be excluded from the list!
        forced_prefix_ids = self.tokenizer(expected_prefix, max_length=512, truncation=True, return_tensors='pt').input_ids[0][:-1]

        logits_processor = PicardLogitsProcessor(
            tokenizer=self.tokenizer,
            eos_token_id=self.config.eos_token_id,
            max_tokens_to_check=self.max_tokens_to_check,
            mode=self.picard_mode,
            forced_prefix_ids=forced_prefix_ids,
            stop_words_ids=self.stop_words_ids
        )

        input = self.tokenizer(raw_input, max_length=512, truncation=True, return_tensors="pt")
        outputs = self.model.generate(
            **input,
            num_beams=8,
            logits_processor=LogitsProcessorList([logits_processor]),
            min_length=10,
            max_length=512,
        )
        return self.tokenizer.batch_decode(outputs, skip_special_tokens=True)
