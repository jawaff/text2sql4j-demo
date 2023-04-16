# Text2SQL4j

## Setup

- Installt Python 3.7 and set `PYTHON_EXECUTABLE` environment variable.

```shell
export PYTHON_EXECUTABLE=/home/jake/miniconda3/bin/python
```

- Run the following command to clone the djl-serving project and install its Python library, which will be used for
running our Huggingface translator.

```shell
sh setup_djl_serving.sh
```

- Run the following command to download the Picard model to `./raw-files/t5.1.1.lm100k.large/`:

```shell
sh download_model.sh
```

- Download JDK 11:

```shell
sudo apt install openjdk-11-jdk
```

- Gradle is used for building and running tests, but the `./gradlew` file should ensure that it doesn't need to be downloaded.

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

The model that we're going to use is a T5 model that has been fine-tuned in the [Picard](https://github.com/ServiceNow/picard) project.
They have two variants: https://huggingface.co/tscholak/3vnuv1vf and https://huggingface.co/tscholak/cxmefzzi. The first
is a smaller model that branched off of the `t5.1.1.lm100k.large` model with ~800 million parameters. The second is the larger
model that branched off of the `t5.3b` model with ~3 billion parameters.

### TorchScript Conversion

(Abandoned due to text generation unsupported in Deep Java Library)

A TorchScript conversion was experimented with to allow the model to be used by different languages. There can be multiple
use cases for why a different language might be used: better multithreading support (i.e. bypassing Python GIL) and
using execution environments better suited for another language (e.g. Android application).

For the TorchScript conversion we're specifically interested in these files from the HuggingFace model: `pytorch_model.bin`, 
`config.json` and `tokenizer.json`. The first two need to be converted to the `TorchScript` format and the `tokenizer.json` 
has the vocabulary and will be used to create a tokenizer.

The `./model-conversion/` directory holds a Jupiyter notebook that was used for converting our downloaded model into
the TorchScript format. It may be out of date since it was abandoned, but was working at one point. After more research
it seems that the TorchScript conversion wasn't entirely correct because it didn't make use of Huggingface's `model.generate()`
function during the tracing. Deep Java Library doesn't yet have text generation support at the moment, so that will not 
be explored further. This experiment would have worked if we only needed classification or a non-autoregressive model.

## Running

### Python Tests

Run the following commands to run the unit tests in `./python-translator/tests/`:

```shell
cd python-translator
python -m unittest discover -s tests -p '*_test.py'
```

### Java Tests

Run the following command to execute the Java unit tests:

```shell
./gradlew test
```