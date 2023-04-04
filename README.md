# Text2SQL4j

## Library Dependencies

### Python

- Huggingface Transformers: Used to load the Huggingface model and tokenizer downloaded from `huggingface.co`.
- Torch: Used to trace the loaded model and export it to the `TorchScript` format.

### Java

- Deep Java Library: The framework to be used for our NLP tasks.
- DJL Torch Engine: `Torch` will be the engine we're using for loading our `TorchScript` Huggingface model. `Deep Java Library` has built-in support for it.
- DJL Huggingface Tokenizers Extension: `Deep Java Library` offers a Huggingface tokenizer extension that is basically a wrapper for a Rust library written by Huggingface.
  This library allows for a Huggingface tokenizer object to be loaded from our `tokenizer.json` file, which is very similar to what we'd normally do in Python.
  To be clear, we don't need any other Huggingface libraries for interacting with our model because it is in a format that `Torch` and `Deep Java Library` can work with.

## Model

The model that we're going to be building off of is [Picard](https://github.com/ServiceNow/picard).
Its models are stored on `huggingface.co` using these [best practices](https://huggingface.co/transformers/v1.2.0/serialization.html#serialization-best-practices)
(mostly) and need to be converted to `TorchScript` so that they're compatible with `Deep Java Library`.
The main difference is that it doesn't have a vocab file and instead has a `tokenizer.json` file, which is what we want anyway for the Huggingface tokenizer extension in `Deep Java Library`.

We're specifically interested in these files from the HuggingFace model: `pytorch_model.bin`, `config.json` and `tokenizer.json`.
The first two need to be converted to the `TorchScript` format and the `tokenizer.json` has the vocabulary and 
will be used to create a tokenizer.


## Running
