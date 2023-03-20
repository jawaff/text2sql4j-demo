from transformers.models.auto import AutoConfig, AutoModelForSeq2SeqLM
import torch

# The name of the picard model on huggingface.co.
model_name = "tscholak/cxmefzzi"
cache_dir = "./transformers_cache"

config = AutoConfig.from_pretrained(
    model_name,
    #cache_dir=cache_dir,
    max_length=512,
    num_beams=4,
    num_beam_groups=1,
    diversity_penalty=0.0
    #use_cache=True
)

model = AutoModelForSeq2SeqLM.from_pretrained(
    model_name,
    config=config,
    #cache_dir=cache_dir
)

# Switch the model to eval model
model.eval()

# An example input you would normally provide to your model's forward() method.
#example = torch.rand(1, 3, 224, 224)
example = torch.rand(512, 600)

# Use torch.jit.trace to generate a torch.jit.ScriptModule via tracing.
traced_script_module = torch.jit.trace(model, example)

# Save the TorchScript model
traced_script_module.save("picard_model.pt")