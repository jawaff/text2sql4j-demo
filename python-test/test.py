import torch
from transformers.models.auto import AutoConfig, AutoTokenizer, AutoModelForSeq2SeqLM

config = AutoConfig.from_pretrained('../raw-files/')
tokenizer = AutoTokenizer.from_pretrained('../raw-files/')
model = AutoModelForSeq2SeqLM.from_pretrained('../raw-files/', config=config)

inputs = ['Get concerts with short names. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id']

inputs = tokenizer(inputs, max_length=max_input_length, truncation=True, return_tensors="pt")
outputs = model.generate(**inputs, num_beams=8, do_sample=True, min_length=10, max_length=512)
decoded_outputs = tokenizer.batch_decode(output, skip_special_tokens=True)
predicted_title = nltk.sent_tokenize(decoded_output.strip())[0]
