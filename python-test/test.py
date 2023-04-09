import torch
from transformers.models.auto import AutoConfig, AutoTokenizer, AutoModelForSeq2SeqLM
from transformers import LogitsProcessor, LogitsProcessorList

class SqlLogitsProcessor(LogitsProcessor):
    def __init__(self, tokenizer):
        self._tokenizer = tokenizer
        # TODO Defined in the config.json, but probably a better way to get it...
        self._eos_token_id = 1
    def __call__(self, input_ids, scores):
        inputs = self._tokenizer.batch_decode(input_ids, skip_special_tokens=True)
        print(inputs)
        print(input_ids.shape)
        cur_len = input_ids.shape[-1]
        if cur_len > 3:
            print("DONE!")
            scores[:, self._eos_token_id] = float("inf")
            print(scores)
        # The processed prediction scores (torch.FloatTensor of shape (batch_size, config.vocab_size))
        return scores # TODO Replace this with updated prediction scores



config = AutoConfig.from_pretrained('../raw-files/t5.1.1.lm100k.large')
tokenizer = AutoTokenizer.from_pretrained('../raw-files/t5.1.1.lm100k.large')
model = AutoModelForSeq2SeqLM.from_pretrained('../raw-files/t5.1.1.lm100k.large', config=config)

logits_processor_list = LogitsProcessorList([SqlLogitsProcessor(tokenizer)])
inputs = ['Get concerts with short names. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id']

inputs = tokenizer(inputs, max_length=512, truncation=True, return_tensors="pt")
outputs = model.generate(
    **inputs,
    num_beams=8,
    logits_processor=logits_processor_list,
    min_length=10,
    max_length=512,
)
print(type(outputs))
decoded_outputs = tokenizer.batch_decode(outputs, skip_special_tokens=True)

print(decoded_outputs)
