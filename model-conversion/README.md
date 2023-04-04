# Model Conversion

The model files usually uploaded to the HuggingFace website aren't usually compatible with Deep Java Library without modification. Deep Java Library requires a TorchScript model in order to load them.
(Here)[https://huggingface.co/docs/transformers/v4.17.0/en/serialization#torchscript] is a good reference on how to convert the model files, but one drawback is that the TorchScript model might not allow
further training depending on the dependencies between the encoder and decoder. Training should be done in Python before the model is saved as the TorchScript format.

## Prerequisites

TODO: Create a requirements.txt file

- Python 3.7
- Pytorch 
- Jupyter Notebook
- Huggingface Transformers


## Execution

Run Jupyter Notebook first and then execute all of the cells within the `convert.ipynb` notebook.