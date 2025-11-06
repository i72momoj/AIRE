from pprint import pp
import random
from math import factorial
from itertools import combinations
import re

def meterCombinaciones(frase:str, valores:list[str], simbolo:str, etiqueta:str):
    frases_resultantes = []
    frases_resultantes_bio = []

    valores_bio = list(map(lambda valor: ' '.join([ ("I-" + etiqueta if elemento.lower() not in ['los'] else elemento) for elemento in valor.split(" ")]), valores))
    valores_bio = list(map(lambda valor: valor.replace("I-" + etiqueta, "B-" + etiqueta, 1), valores_bio))

    #for frase in frases_generadas:
    cont = 1
    for valor in valores:
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
        frases_combinadas_bio = [frase] * int(factorial(n) / (factorial(r) * factorial(n-r)))

        frases_resultantes += list(map(lambda frase, grupo: frase.replace(simbolo, grupo), frases_combinadas, grupos))
        frases_resultantes_bio += list(map(lambda frase, grupo: frase.replace(simbolo, grupo), frases_combinadas_bio, grupos_bio))
        
        cont+=1

    #pp(f"{frases_resultantes}, {frases_resultantes_bio}")
    return frases_resultantes, frases_resultantes_bio

def combinar(frases: list, frases_bio:list, variables:list, etiqueta: str, simbolo:str):
    '''
    Genera frases y etiquetas BIO a partir de una o varias plantillas, un único símbolo y una lista de valores a sustituir por ese símbolo.

    Parámetros
    ----------
    - frases: lista de frases plantilla
    - frases_bio: lista de frases con etiquetas BIO aplicadas parcialmente
    - variables: lista de valores que aplicar a las frases plantilla
    - simbolo: símbolo dentro de la frase plantilla por la que sustituir cada uno de los valores en *variables*.

    Devuelve
    --------
    - frases_generadas: lista de frases generadas a partir de la combinación de las frases plantilla y las variables
    - frases_generadas_bio: lista de etiquetas BIO para cada una de las frases en *frases_generadas*
    '''

    frases_generadas = []
    frases_generadas_bio = []

    for frase, frase_bio in zip(frases, frases_bio):

        # Generamos tantas frases como combinaciones haya, para cada frase sustituímos el símbolo por uno de los valores
        # y las añadimos a frases generadas
        frases_combinacion = [frase] * len(variables)
        frases_generadas += list(map(lambda frase, variable: frase.replace(simbolo, variable), frases_combinacion, variables))

        # Para las etiquetas, también generamos tantas frases como combinaciones haya, pero en vez de sustituir el símbolo
        # por un valor, multiplicamos el símbolo a cuantas palabras tenga el valor (p.e. si el valor es "treinta y cinco" y el
        # símbolo %, en la frase pasará de aparecer como "[...] % [...]" a "[...] % % % [...]"). Estos entonces son sustituídos
        # por su correspondiente etiqueta BIO, primero todas son sustituídas por I-<etiqueta> y luego la primera del conjunto
        # es sustituída por B-<etiqueta>
        frases_generadas_bio_raw = [frase_bio] * len(variables)
        frases_generadas_bio_raw = list(map(lambda frase, variable: frase.replace(simbolo, ' '.join([simbolo] * len(variable.split()))), frases_generadas_bio_raw, variables))
        frases_generadas_bio_raw = list(map(lambda bio: bio.replace(simbolo, "I-" + etiqueta), frases_generadas_bio_raw))
        frases_generadas_bio += list(map(lambda bio: bio.replace("I-" + etiqueta, "B-" + etiqueta, 1), frases_generadas_bio_raw))

    return frases_generadas, frases_generadas_bio

def generar_frases(plantilla: str, frases:list, frases_bio:list, variables: list, subconjunto: int = 0, debug: bool = False):
    '''
    Genera todas las frases y las etiquetas BIO correspondientes a dichas frases a partir de una única frase plantilla y una lista de variables.

    Parámetros
    ----------
    - plantilla: frase plantilla
    - frases: lista de frases donde añadir las frases generadas
    - frases_bio: lista de frases donde añadir las frases generadas con etiquetas BIO aplicadas en su totalidad
    - variables: diccionario con los valores de las variables a incluir en las frases, el símbolo de sustitución en la frase plantilla y su etiqueta BIO correspondiente
    - debug: indica si debe mostrar o no por pantalla las frases y etiquetas generadas
    '''

    # Obetenemos los símbolos dentro de la plantilla que se van a sustitir por palabras (%, &, $...)
    simbolos = [variables[i]['simbolo'] for i in range(len(variables))]

    # Las frases generadas comienzan siendo únicamente la plantilla, donde toda palabra que no sea
    # un símbolo es sustituido por el tag O (ignorar)
    frases_generadas = [plantilla]
    frases_generadas_bio = [' '.join(['O' if item not in simbolos else item for item in plantilla.split()])]

    for variable in list(variables):
        if variable['combinar'] == True:
            frases_generadas, frases_generadas_bio = meterCombinaciones(
                frase=plantilla, 
                valores=variable['valores'], 
                simbolo=variable['simbolo'],
                etiqueta=variable['bio']
            )
            #frases_generadas_bio = list(map(lambda frase: frase.replace(variable['simbolo'], 'B-'+ variable['bio']), frases_generadas))
            frases_generadas_bio = list(map(lambda frase: ' '.join(['O' if item not in simbolos and item not in ['B-'+variable['bio'],'I-'+variable['bio']] else item for item in frase.split()]), frases_generadas_bio))
            variables.remove(variable)

    #pp(f'COMBINACIÓN: frases generadas: {len(frases_generadas)}, etiquetas: {len(frases_generadas_bio)}')

    # Mediante la función combinar(), a partir de la plantilla y las distintas variables generamos las frases y sus etiquetas
    for elemento in variables:
        frases_generadas, frases_generadas_bio = combinar(
            frases=frases_generadas, 
            frases_bio=frases_generadas_bio, 
            simbolo=elemento['simbolo'], 
            etiqueta=elemento['bio'], 
            variables=elemento['valores']
        )

    #pp(f'GENERACION: frases generadas: {len(frases_generadas)}, etiquetas: {len(frases_generadas_bio)}')

    # Si se ha especificado un subconjunto, del total de las frases generadas seleccionamos un subconjunto
    # aleatorio pero constante del total
    if subconjunto > 0:
        nfrases_original = len(frases_generadas)
        multiplicador = pow(10, len(f"{nfrases_original}") + 1)

        frases_generadas_filtradas = []
        frases_generadas_bio_filtradas = []

        for cont in range(subconjunto):
            random.seed(cont)
            indice = int(random.random() * multiplicador) % nfrases_original

            frases_generadas_filtradas.append(frases_generadas[indice])
            frases_generadas_bio_filtradas.append(frases_generadas_bio[indice])
            frases.append(list(frases_generadas[indice].split()))
            frases_bio.append(list(frases_generadas_bio[indice].split()))

        frases_generadas = frases_generadas_filtradas
        frases_generadas_bio = frases_generadas_bio_filtradas

    else:
        for frase in frases_generadas:
            frases.append(list(frase.split()))
        
        for frase in frases_generadas_bio:
            frases_bio.append(list(frase.split()))

    # Si se activa el modo debug, muestra tanto las frases como las etiquetas generadas por pantalla
    if debug:
        pp(list(map(lambda frase, bio: str(f"{frase},{bio}").split(','), frases_generadas, frases_generadas_bio)))


dataset = []        # Almacenará las frases que conformen el dataset
dataset_bio = []    # Por cada frase del dataset, esta lista contendrá sus etiquetas BIO para clasificación de tokens

# ------------------------------------------------------------------#
#                   GENERACIÓN DE FRASES: ALARMAS                   #
#-------------------------------------------------------------------#

horas = ['una', 'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete', 'ocho', 'nueve', 'diez', 'once', 'doce']

minutos = [
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
]

rel_horas = [
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
        'dieciseis',
        'diecisiete',
        'dieciocho',
        'diecinueve',
        'veinte',
        'ventiún',
        'ventidós',
        'ventitrés',
        'venticuatro'
]

rel_minutos = [
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
]

rel_segundos = [
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
        'veintiún',  
        'veintidós',  
        'veintitrés',  
        'veinticuatro',  
        'veinticinco',  
        'veintiséis',  
        'veintisiete',  
        'veintiocho',  
        'veintinueve',  
        'treinta',  
        'treinta y un',  
        'treinta y dos',  
        'treinta y tres',  
        'treinta y cuatro',  
        'treinta y cinco',  
        'treinta y seis',  
        'treinta y siete',  
        'treinta y ocho',  
        'treinta y nueve',  
        'cuarenta',  
        'cuarenta y un',  
        'cuarenta y dos',  
        'cuarenta y tres',  
        'cuarenta y cuatro',  
        'cuarenta y cinco',  
        'cuarenta y seis',  
        'cuarenta y siete',  
        'cuarenta y ocho',  
        'cuarenta y nueve',  
        'cincuenta',  
        'cincuenta y un',  
        'cincuenta y dos',  
        'cincuenta y tres',  
        'cincuenta y cuatro',  
        'cincuenta y cinco',  
        'cincuenta y seis',  
        'cincuenta y siete',  
        'cincuenta y ocho',  
        'cincuenta y nueve'
]

minutos_reloj = [ 'cuarto', 'media', 'tres cuartos' ]

minutos_reloj_menos = [
    'menos cinco',
    'menos diez',
    'menos cuarto',
    'menos veinte',
    'menos veinticinco'
]

minutos_cero = [
        'uno',  
        'dos',  
        'tres',  
        'cuatro',  
        'cinco',  
        'seis',  
        'siete',  
        'ocho',  
        'nueve'
]

mnt = ['mañana', 'tarde', 'noche']

dias = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
dias_los = ['los lunes', 'los martes', 'los miércoles', 'los jueves', 'los viernes', 'los sábado', 'los domingo']
dias_relativos = ['hoy', 'mañana', 'pasado mañana']

# Generamos frases a partir de una frase "plantilla" y el conjunto de variables y las añadimos al dataset

generar_frases(
    plantilla='ponme una alarma a las %', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las % horas y $ minutos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las % $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma en $ horas', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma en $ minutos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma en $ segundos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELS', 'valores': rel_segundos, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma en $ horas y % minutos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False},
        {'simbolo': '%', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma en $ horas y %', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False},
        {'simbolo': '%', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma en $ minutos y % segundos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '$', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
        {'simbolo': '%', 'bio': 'RELS', 'valores': rel_segundos, 'combinar': False}
        ],
    debug=True
    )

generar_frases(
    plantilla='ponme una alarma el @ a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '@', 'bio': 'DSEM', 'valores': dias, 'combinar': False},
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma el @ a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '@', 'bio': 'DSEM', 'valores': dias, 'combinar': False},
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma el @ a las % $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '@', 'bio': 'DSEM', 'valores': dias, 'combinar': False},
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False}
        ]
    )
generar_frases(
    plantilla='ponme una alarma a las % de la &', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False},
        {'simbolo': '&', 'bio': 'MTN', 'valores': mnt, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma el @ a las % y $ de la &', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '@', 'bio': 'DSEM', 'valores': dias, 'combinar': False},
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False},
        {'simbolo': '&', 'bio': 'MTN', 'valores': mnt, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las % $ de la &', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False},
        {'simbolo': '&', 'bio': 'MTN', 'valores': mnt, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma cada & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False},
        {'simbolo': '&', 'bio': 'MTN', 'valores': mnt, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma cada & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False},
        {'simbolo': '&', 'bio': 'MTN', 'valores': mnt, 'combinar': False}
        ]
    )

generar_frases(
    plantilla='ponme una alarma a las a las % cero $',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HORA', 'valores': rel_horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_cero, 'combinar': False},
    ]
)

generar_frases(
    plantilla='ponme una alarma los & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ],
    subconjunto=1500
)

generar_frases(
    plantilla='ponme una alarma los & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False}
        ],
    subconjunto=200
)

generar_frases(
    plantilla='ponme una alarma los & a las % $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False}
        ],
    subconjunto=500,
)


generar_frases(
    plantilla='ponme una alarma & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_los, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ],
    subconjunto=1500
)

generar_frases(
    plantilla='ponme una alarma & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_los, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False}
        ],
    subconjunto=200
)

generar_frases(
    plantilla='ponme una alarma & a las % $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_los, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False}
        ],
    subconjunto=500,
)

generar_frases(
    plantilla='ponme una alarma & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_relativos, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ],
    subconjunto=1500,
    debug=True
)

generar_frases(
    plantilla='ponme una alarma & a las % y $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_relativos, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj, 'combinar': False}
        ],
    debug=True
)

generar_frases(
    plantilla='ponme una alarma & a las % $', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_relativos, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos_reloj_menos, 'combinar': False}
        ]
)

generar_frases(
    plantilla='ponme una alarma & a las % y $ minutos', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '&', 'bio': 'DSEM', 'valores': dias_relativos, 'combinar': True}, 
        {'simbolo': '%', 'bio': 'HORA', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MIN', 'valores': minutos, 'combinar': False}
        ],
    subconjunto=1000,
    debug=True
)

# ------------------------------------------------------------------#
#                           TEMPORIZADORES                          #
#-------------------------------------------------------------------#

generar_frases(
    plantilla='avísame en % segundos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELS', 'valores': rel_segundos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % minutos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % horas',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en %',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % minutos y $ segundos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'RELS', 'valores': rel_segundos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % horas y $ minutos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % horas y $ minutos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % horas y $',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'RELM', 'valores': minutos_reloj, 'combinar': False},
    ]
)

generar_frases(
    plantilla='avísame en % horas $ minutos y & segundos',
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'RELH', 'valores': rel_horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'RELM', 'valores': rel_minutos, 'combinar': False},
        {'simbolo': '&', 'bio': 'RELS', 'valores': rel_segundos, 'combinar': False},
    ],
    subconjunto=5000
)

'''generar_frases(
    plantilla='cambia la alarma de las % y $ a las ) y !', 
    frases=dataset, 
    frases_bio=dataset_bio, 
    variables = [
        {'simbolo': '%', 'bio': 'HANT', 'valores': horas, 'combinar': False}, 
        {'simbolo': '$', 'bio': 'MANT', 'valores': minutos, 'combinar': False},
        {'simbolo': ')', 'bio': 'HDESP', 'valores': horas, 'combinar': False}, 
        {'simbolo': '!', 'bio': 'MDESP', 'valores': minutos, 'combinar': False},
        ],
    debug=True,
    subconjunto=3000
)'''

rel_reloj = ['media', 'cuarto', 'tres cuartos']

# Comprobamos resultados
print(f"{len(dataset)} frases generadas para el dataset\n{len(dataset_bio)} frases aplicadas etiquetas BIO")

# Volcamos resultados en un fichero de texto
try:
    with open("dataset.py", "w", newline='\n') as f:
        f.write(f"datos = {{\n\"comandos\":\n{str(dataset)},\n\n\"tokens\": {str(dataset_bio)}\n}}")

    print("Frases generadas volcadas con éxito en el fichero")

except:
    print("Error al volcar los datos")
