# Text2SQL4j

## Model

The model that we're going to be building off of is [Picard](https://github.com/ServiceNow/picard).
Its models are stored on `huggingface.co` using these [best practices](https://huggingface.co/transformers/v1.2.0/serialization.html#serialization-best-practices)
(mostly) and need to be converted to `TorchScript` so that they're compatible with `Deep Java Library`.
The main difference is that it doesn't have a vocab file and instead has a `tokenizer.json` file, which is what we want anyway.

We're specifically interested in these files from the HuggingFace model: `pytorch_model.bin`, `config.json` and `tokenizer.json`.
The first two need to be converted to the `TorchScript` format and the `tokenizer.json` has the vocabulary and 
will be used to create a tokenizer.


## Running
