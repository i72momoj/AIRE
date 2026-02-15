import random
from itertools import combinations
from math import factorial
from pprint import pp

def combinar(frase: str, vals: list, simbolo:str, combinado:bool):

    frases = [frase]

    # Si la frase ya está combinanda, no hace falta
    # generar duplicados
    if combinado == False:
        frases = [frase] * len(vals)

    # Sustituímos el símbolo indicado en las frases generadas
    # por los valores pasados
    frases = list(map(lambda frase, val: frase.replace(simbolo, val), frases, vals))
    
    return frases

def get_random_indices(lista: list, nels:int):
    '''
    Dada una lista de valores y un número especificado de índices, 
    devuelve una lista de ínices aleatorios y constantes, es decir, los índices
    escogidos no cambiarán a menos que cambie o bien la lista, o el número de
    índices a devolver.

    Parámetros
    ----------
    - lista: lista de valores
    - nels: número de índices a sacar
    '''

    indices = []
    i = 0
    seed = 0
    while i < nels:

        random.seed(seed)
        indice = int((random.random() * 1000 * nels ) % len(lista))

        if indice not in indices:
            indices = indices + [indice]
            i += 1

        seed += 1
    
    return indices

def metercomb(
        valores: list,
        valores_bio: list,
        simbolo: str,
        frase:str,
        frase_bio:str,
        subgrupo:int = 0
        ):

    frases_resultantes = []
    frases_resultantes_bio = []

    cont = 1
    for _ in valores:
        n = len(valores)
        r = cont

        grupos = list(combinations(valores, cont))
        grupos_bio = list(combinations(valores_bio, cont))

        if(len(grupos) > 0 and len(grupos[0]) > 1):
            grupos = list(map(lambda grupo: grupo[:-1] + ('y '.join(["", grupo[-1]]),), grupos))
            grupos_bio = list(map(lambda grupo: grupo[:-1] + ('O '.join(["", grupo[-1]]),), grupos_bio))

        grupos = [list(grupo) for grupo in grupos]
        grupos_bio = [list(grupo) for grupo in grupos_bio]

        grupos = list(map(lambda grupo: ' '.join([elemento for elemento in grupo]), grupos))
        grupos_bio = list(map(lambda grupo: ' '.join([elemento for elemento in grupo]), grupos_bio))

        frases_combinadas = [frase] * int(factorial(n) / (factorial(r) * factorial(n-r)))
        frases_combinadas_bio = [frase_bio] * int(factorial(n) / (factorial(r) * factorial(n-r)))

        frases_resultantes += list(map(lambda frase, grupo: frase.replace(simbolo, grupo), frases_combinadas, grupos))
        frases_resultantes_bio += list(map(lambda frase, grupo: frase.replace(simbolo, grupo), frases_combinadas_bio, grupos_bio))
        
        cont+=1

    if subgrupo > 0:
        indices = get_random_indices(frases_resultantes, subgrupo)
        frases_resultantes = [frases_resultantes[i] for i in indices]
        frases_resultantes_bio = [frases_resultantes_bio[i] for i in indices]

    return frases_resultantes, frases_resultantes_bio

def generar(
        plantilla:str, 
        listas:list, 
        simbolos:list, 
        ds:list, 
        dsbio:list, 
        subgrupos: list = [],
        comb: list = [],
        debug: bool = False
        ):
    
    # Sustituímos por la etiqueta BIO "O" (descartar) todo lo que no vaya a ser sustituído
    frase_bio = ' '.join(list(map(lambda palabra: "O" if palabra not in simbolos else palabra, plantilla.split(' '))))

    # En Python los diccionarios se pasan por referencia por lo que se ve, y para copiarlos
    # hace falta hacerlo manualmente, así que lo hacemos
    variables = []
    for dic in listas:
        variables += [{"vals": dic["vals"], "bio": dic["bio"]}]

    if subgrupos == []:
        subgrupos = [0 for _ in variables]

    if comb == []:
        comb = [False for _ in variables]

    frases_res = [plantilla]
    bio = [frase_bio]

    # En caso de que se hayan definido grupos
    for lista, nels, c in zip(variables, subgrupos, comb):

        if(c == False and nels > 0):
            # Obtenemos índices aleatorios pero CONSTANTES
            indices = get_random_indices(lista["vals"], nels)

            # Filtramos los elementos de las variables
            lista["vals"] = [lista["vals"][indice] for indice in indices]
            lista["bio"] = [lista["bio"][indice] for indice in indices]

    for lista, nels, simbolo, c in zip(variables, subgrupos, simbolos, comb):
        if c == True:
            frases_res, bio = metercomb(
                valores=lista["vals"],
                valores_bio=lista["bio"],
                simbolo=simbolo,
                frase=plantilla,
                frase_bio=frase_bio,
                subgrupo=nels
                )

    for lista, simbolo, c in zip(variables, simbolos, comb):
        frases_res = [l for listas_generadas in list(map(lambda f: combinar(f, lista["vals"], simbolo, c), frases_res)) for l in listas_generadas]
        bio = [l for listas_generadas in list(map(lambda f: combinar(f, lista["bio"], simbolo, c), bio)) for l in listas_generadas]

    if debug:
        pp(frases_res)
        #pp(bio)
    
    ds += [ frase.split(" ") for frase in frases_res ]
    dsbio += [ frase.split(" ") for frase in bio ]


hora = {
    "vals": ['una', 'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete', 'ocho', 'nueve', 'diez', 'once', 'doce'],
    "bio" : ["B-HORA", "B-HORA", "B-HORA","B-HORA", "B-HORA", "B-HORA","B-HORA", "B-HORA", "B-HORA","B-HORA", "B-HORA", "B-HORA"]
}

hora_act = {
    "vals": ['una', 'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete', 'ocho', 'nueve', 'diez', 'once', 'doce'],
    "bio" : ["B-ACTH", "B-ACTH", "B-ACTH","B-ACTH", "B-ACTH", "B-ACTH","B-ACTH", "B-ACTH", "B-ACTH","B-ACTH", "B-ACTH", "B-ACTH"]
}

hora_24 = {
    "vals": [
        'cero cero',
        'una',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiuno',  
        'veintidós',  
        'veintitrés',
        ],
    "bio" : [
        'O B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',  
        'B-HORA',
        'B-HORA',  
        'B-HORA',
    ]
}

hora_24_rel = {
    "vals": [
        'cero',
        'una',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiún',  
        'veintidós',  
        'veintitrés',
        ],
    "bio" : [
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',  
        'B-RELH',
        'B-RELH',  
        'B-RELH',
    ]
}

hora_24_act = {
    "vals": [
        'cero',
        'una',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiún',  
        'veintidós',  
        'veintitrés',
        ],
    "bio" : [
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',  
        'B-ACTH',
        'B-ACTH',  
        'B-ACTH',
    ]
}

min = {
    "vals": [
        'un',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiuno',  
        'veintidós',  
        'veintitrés',  
        'veinticuatro',  
        'veinticinco',  
        'veintiséis',  
        'veintisiete',  
        'veintiocho',  
        'veintinueve',  
        'treinta',  
        'treinta y uno',  
        'treinta y dos',  
        'treinta y tres',  
        'treinta y cuatro',  
        'treinta y cinco',  
        'treinta y seis',  
        'treinta y siete',  
        'treinta y ocho',  
        'treinta y nueve',  
        'cuarenta',  
        'cuarenta y uno',  
        'cuarenta y dos',  
        'cuarenta y tres',  
        'cuarenta y cuatro',  
        'cuarenta y cinco',  
        'cuarenta y seis',  
        'cuarenta y siete',  
        'cuarenta y ocho',  
        'cuarenta y nueve',  
        'cincuenta',  
        'cincuenta y uno',  
        'cincuenta y dos',  
        'cincuenta y tres',  
        'cincuenta y cuatro',  
        'cincuenta y cinco',  
        'cincuenta y seis',  
        'cincuenta y siete',  
        'cincuenta y ocho',  
        'cincuenta y nueve'
        ],
    "bio" : [
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN',  
        'B-MIN I-MIN I-MIN'
    ]
}

min_rel = {
    "vals": [
        'cero',
        'un',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiuno',  
        'veintidós',  
        'veintitrés',  
        'veinticuatro',  
        'veinticinco',  
        'veintiséis',  
        'veintisiete',  
        'veintiocho',  
        'veintinueve',  
        'treinta',  
        'treinta y uno',  
        'treinta y dos',  
        'treinta y tres',  
        'treinta y cuatro',  
        'treinta y cinco',  
        'treinta y seis',  
        'treinta y siete',  
        'treinta y ocho',  
        'treinta y nueve',  
        'cuarenta',  
        'cuarenta y uno',  
        'cuarenta y dos',  
        'cuarenta y tres',  
        'cuarenta y cuatro',  
        'cuarenta y cinco',  
        'cuarenta y seis',  
        'cuarenta y siete',  
        'cuarenta y ocho',  
        'cuarenta y nueve',  
        'cincuenta',  
        'cincuenta y uno',  
        'cincuenta y dos',  
        'cincuenta y tres',  
        'cincuenta y cuatro',  
        'cincuenta y cinco',  
        'cincuenta y seis',  
        'cincuenta y siete',  
        'cincuenta y ocho',  
        'cincuenta y nueve'
        ],
    "bio" : [
        'B-RELM',
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM',  
        'B-RELM I-RELM I-RELM'
    ]
}

min_act = {
    "vals": [
        'cero',
        'un',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiuno',  
        'veintidós',  
        'veintitrés',  
        'veinticuatro',  
        'veinticinco',  
        'veintiséis',  
        'veintisiete',  
        'veintiocho',  
        'veintinueve',  
        'treinta',  
        'treinta y uno',  
        'treinta y dos',  
        'treinta y tres',  
        'treinta y cuatro',  
        'treinta y cinco',  
        'treinta y seis',  
        'treinta y siete',  
        'treinta y ocho',  
        'treinta y nueve',  
        'cuarenta',  
        'cuarenta y uno',  
        'cuarenta y dos',  
        'cuarenta y tres',  
        'cuarenta y cuatro',  
        'cuarenta y cinco',  
        'cuarenta y seis',  
        'cuarenta y siete',  
        'cuarenta y ocho',  
        'cuarenta y nueve',  
        'cincuenta',  
        'cincuenta y uno',  
        'cincuenta y dos',  
        'cincuenta y tres',  
        'cincuenta y cuatro',  
        'cincuenta y cinco',  
        'cincuenta y seis',  
        'cincuenta y siete',  
        'cincuenta y ocho',  
        'cincuenta y nueve'
        ],
    "bio" : [
        'B-ACTM',
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM',  
        'B-ACTM I-ACTM I-ACTM'
    ]
}

seg_rel = {
    "vals": [
        'cero',
        'un',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve',  
        'diez',  
        'once',  
        'doce',  
        'trece',  
        'catorce',  
        'quince',  
        'dieciséis',  
        'diecisiete',  
        'dieciocho',  
        'diecinueve',  
        'veinte',  
        'veintiuno',  
        'veintidós',  
        'veintitrés',  
        'veinticuatro',  
        'veinticinco',  
        'veintiséis',  
        'veintisiete',  
        'veintiocho',  
        'veintinueve',  
        'treinta',  
        'treinta y uno',  
        'treinta y dos',  
        'treinta y tres',  
        'treinta y cuatro',  
        'treinta y cinco',  
        'treinta y seis',  
        'treinta y siete',  
        'treinta y ocho',  
        'treinta y nueve',  
        'cuarenta',  
        'cuarenta y uno',  
        'cuarenta y dos',  
        'cuarenta y tres',  
        'cuarenta y cuatro',  
        'cuarenta y cinco',  
        'cuarenta y seis',  
        'cuarenta y siete',  
        'cuarenta y ocho',  
        'cuarenta y nueve',  
        'cincuenta',  
        'cincuenta y uno',  
        'cincuenta y dos',  
        'cincuenta y tres',  
        'cincuenta y cuatro',  
        'cincuenta y cinco',  
        'cincuenta y seis',  
        'cincuenta y siete',  
        'cincuenta y ocho',  
        'cincuenta y nueve'
        ],
    "bio" : [
        'B-RELS', 
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS',  
        'B-RELS I-RELS I-RELS'
    ]
}

min_reloj = {
    "vals": ["cuarto", "media", "tres cuartos"],
    "bio": ["B-MIN", "B-MIN", "B-MIN I-MIN"]
}

min_reloj_rel = {
    "vals": ["cuarto", "media", "tres cuartos"],
    "bio": ["B-RELM", "B-RELM", "B-RELM I-RELM"]
}

min_reloj_act = {
    "vals": ["cuarto", "media", "tres cuartos"],
    "bio": ["B-ACTM", "B-ACTM", "B-ACTM I-ACTM"]
}

min_menos = {
    "vals": ["menos cinco", "menos diez", "menos cuarto", "menos veinte", "menos veinticinco"],
    "bio": ["B-MIN I-MIN", "B-MIN I-MIN","B-MIN I-MIN","B-MIN I-MIN","B-MIN I-MIN"]
}

min_menos_act = {
    "vals": ["menos cinco", "menos diez", "menos cuarto", "menos veinte", "menos veinticinco"],
    "bio": ["B-ACTM I-ACTM", "B-ACTM I-ACTM","B-ACTM I-ACTM","B-ACTM I-ACTM","B-ACTM I-ACTM"]
}

min_cero = {
    "vals": [
        'cero',
        'uno',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve'],
    "bio": [
        "B-MIN",  
        "B-MIN",  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN',  
        'B-MIN']
}

mtn = {"vals":['mañana', 'tarde', 'noche'], "bio":["B-MTN", "B-MTN", "B-MTN"]}

dias = {
    "vals": ["lunes", "martes", "miércoles", "jueves", "viernes","sábado","domingo"],
    "bio": ["B-DSEM", "B-DSEM", "B-DSEM", "B-DSEM", "B-DSEM", "B-DSEM", "B-DSEM"]
}

dias_los = {
    "vals": ['los lunes', 'los martes', 'los miércoles', 'los jueves', 'los viernes', 'los sábados', 'los domingos'],
    "bio": ["O B-DSEM", "O B-DSEM", "O B-DSEM", "O B-DSEM", "O B-DSEM", "O B-DSEM", "O B-DSEM"]
    }

dias_rel = {
    "vals": ['hoy', 'mañana', 'pasado mañana'],
    "bio": ["B-DSEM", "B-DSEM", "B-DSEM I-DSEM"]
    }

temp_reloj = {
    "vals": [
        "un cuarto de hora", 
        "cuarto de hora", 
        "media hora",
        "una media hora",
        "tres cuartos de hora", 
        "unos tres cuartos de hora"
        ],
    "bio": [
        "O B-RELM O O",
        "B-RELM O O", 
        "B-RELM O",
        "O B-RELM O",
        "B-RELM I-RELM O O", 
        "O B-RELM I-RELM O O"]
}

relleno = {
    "vals": [
        "o así", 
        "más o menos", 
        "exactos",
        "por favor",
        "gracias",
        "muchas gracias",
        "sin falta", 
        "o te desinstalo",
        "que si no se me quema la comida",
        "o vas a ver lo que es bueno",
        "o te vas a enterar",
        "y que no se te pase"
        ],
    "bio": [
        "O O", 
        "O O O", 
        "O",
        "O O",
        "O",
        "O O",
        "O O", 
        "O O O",
        "O O O O O O O O",
        "O O O O O O O O",
        "O O O O O",
        "O O O O O O"
        ]
}

acciones = {
    "vals": [
        "para",
        "borra",
        "apaga",
        "suprime",
        "quita",
        "elimina",
        "anula",
        "destruye",
        "desactiva",
        "cancela",
        "enciende",
        "activa",
        "reactiva",
        "pon",
        "desanula",
        "inicia",
        "reanuda",
        "acciona"
    ],
    "bio": [
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION"
    ]
}

acciones_inf = {
    "vals": [
        "parar",
        "borrar",
        "apagar",
        "suprimir",
        "quitar",
        "eliminar",
        "anular",
        "destruir",
        "desactivar",
        "cancelar",
        "encender",
        "activar",
        "reactivar",
        "poner",
        "desanular",
        "iniciar",
        "reanudar",
        "accionar"
    ],
    "bio": [
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION",
        "B-ACCION"
    ]
}