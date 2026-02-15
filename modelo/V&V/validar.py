import onnxruntime as ort
import numpy as np
from transformers import AutoTokenizer
import torch
from pprint import pp

dirmodelo = "./onnx"

def validar_resultados(text:str, bio:str, dir:str):
    tags = ["O", 
            "B-DSEM",
            "I-DSEM",
            "B-HORA", 
            "I-HORA", 
            "B-MIN", 
            "I-MIN", 
            "B-RELH", 
            "I-RELH", 
            "B-RELM", 
            "I-RELM", 
            "B-RELS", 
            "I-RELS", 
            "B-MTN", 
            "I-MTN", 
            "B-ACTH", 
            "I-ACTH",
            "B-ACTM", 
            "I-ACTM",
            "B-ACCION",
            "I-ACCION"
            ]

    id2tag = {id: tag for id, tag in enumerate(tags)}

    # cargamos el modelo y el tokenizer del model cuantizado
    session = ort.InferenceSession(f"{dirmodelo}/model.quant.onnx", providers=["CPUExecutionProvider"])
    tokenizer = AutoTokenizer.from_pretrained(dirmodelo)

    # Los tokenizamos
    tokens = tokenizer(text, padding=True, truncation=True, return_tensors="np")  # Convert to NumPy array

    # Ensure input tensor shape matches what the model expects
    input_ids = tokens["input_ids"].astype(np.int64)  # ONNX models often require int64 inputs
    attention_mask = tokens["attention_mask"].astype(np.int64)

    # Preparamos los inputs para pasárselos al modelo
    inputs = {
        "input_ids": input_ids,
        "attention_mask": attention_mask
    }

    # Ejecutamos las predicciones y obtenemos los resultados
    outputs = session.run(None, inputs)

    logits = torch.tensor(outputs[0])  # Convert output to a PyTorch tensor
    predictions = torch.argmax(logits, dim=-1)  # Get predicted class index

    # Print tokens and predictions
    tokens_inferidos = tokenizer.convert_ids_to_tokens(inputs['input_ids'][0])
    tags_inferidos = [id2tag[id] for id in predictions[0].tolist()]

    bio = bio.split(' ')

    tokens_esperados = []
    tokens_esperados += [token for lista in [tokenizer.convert_ids_to_tokens(tokenizer(palabra)["input_ids"])[1:-1] for palabra in text.split(' ')] for token in lista]

    tags_esperados = []
    b = 0
    for i in range(len(tokens_esperados)):
        if(tokens_esperados[i][0] != "#"):
            tags_esperados.append(bio[b])
            i += 1
            b += 1

        else:
            tags_esperados.append(bio[b-1])
            i += 1
            
    tokens_esperados = ["[CLS]"] + tokens_esperados + ["[SEP]"]
    tags_esperados = ["O"] + tags_esperados + ["O"]

    tokens_coinciden = True
    tags_coinciden = True

    pp(tokens_esperados)
    pp(tags_esperados)

    if(len(tokens_esperados) != len(tokens_inferidos)):
        pp("ERROR: el número de tokens inferido no coincide")
        tokens_coinciden = False

    if(len(tags_esperados) != len(tags_inferidos)):
        pp("ERROR: el número de etiquetas inferido no coincide")
        tags_coinciden = False

    for token_esperado, token_inferido in zip(tokens_esperados, tokens_inferidos):
        if(token_esperado != token_inferido):
            pp("ERROR: los tokens inferidos no coinciden")
            tokens_coinciden = False

    for tag_esperado, tag_inferido in zip(tags_esperados, tags_inferidos):
        if(tag_esperado != tag_inferido):
            pp("ERROR: las etiquetas inferidos no coinciden")
            tags_coinciden = False

    if(tokens_coinciden and tags_coinciden):
        pp("El resultado obtenido es el resultado esperado")



