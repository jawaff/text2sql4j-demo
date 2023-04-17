import sys
import os
import torch
from transformers.models.auto import AutoConfig, AutoTokenizer, AutoModelForSeq2SeqLM
from transformers import LogitsProcessor, LogitsProcessorList
from logits import PicardLogitsProcessor


def restrict_vocab_by_prefix(batch_id, beam_ids, expected_prefix_ids):
    if len(beam_ids) < len(expected_prefix_ids):
        return [expected_prefix_ids[len(beam_ids)]]
    return None

class Translator:
    def __init__(self, model_path, **kwargs):
        self.config = AutoConfig.from_pretrained(
            model_path,
            num_beams=3,
            num_beam_groups=1,
            diversity_penalty=0.0,
        )
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(model_path, config=self.config, **kwargs)

        self.max_tokens_to_check = 3

        stop_words = ['group by', 'limit', 'offset']
        # </s> should be excluded from these lists!
        # Each word is converted into a separate list of ids
        self.stop_words_ids = [words_ids[:-1] for words_ids in self.tokenizer(stop_words).input_ids]

    def translate(self, expected_prefix, raw_input, is_incremental=False):
        # </s> should be excluded from the list!
        forced_prefix_ids = self.tokenizer(expected_prefix, max_length=512, truncation=True).input_ids[:-1]

        logits_processor = PicardLogitsProcessor(
            tokenizer=self.tokenizer,
            eos_token_id=self.config.eos_token_id,
            max_tokens_to_check=self.max_tokens_to_check,
            forced_prefix_ids=forced_prefix_ids,
            stop_words_ids=self.stop_words_ids,
            is_incremental=is_incremental,
        )

        input = self.tokenizer(raw_input, padding=True, max_length=512, truncation=True, return_tensors="pt")
        with torch.no_grad():
            outputs = self.model.generate(
                **input,
                prefix_allowed_tokens_fn=lambda batch_id, beam_ids: restrict_vocab_by_prefix(batch_id, beam_ids, forced_prefix_ids),
                logits_processor=LogitsProcessorList([logits_processor]),
                max_new_tokens=512,
            )
        return self.tokenizer.batch_decode(outputs, skip_special_tokens=True)

# Intended to be used by a spark job.
if __name__ == "__main__":
    expected_prefix = '<pad>' + sys.argv[1]
    raw_input = [sys.argv[2]]

    dir_path = os.path.dirname(os.path.realpath(__file__))
    translator = Translator(os.path.join(dir_path, '../spark-data/t5.1.1.lm100k.large'))
    print(translator.translate(expected_prefix, raw_input))
