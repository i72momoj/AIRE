from pygendataset import *
from pprint import pp

'''
Posibles valores
----------------

hora        -> una, dos... doce
hora_24     -> cero cero, una, dos... veintitrés
min         -> un, dos... cincuenta y nueve
seg_rel     -> un, dos... cincuenta y nueve
min_reloj   -> cuarto, media, tres cuartos
min_menos   -> cinco, diez, cuarto... veinticinco
min_cero    -> cero, uno... nueve
mtn         -> mañana, tarde, noche
dias        -> lunes, martes... domingo
dias_los    -> los lunes, los martes... los domingos
dias_rel    -> hoy, mañana, pasado mañana

'''

datos = []      # Todas las frases que formarán el dataset
etiquetas = []  # Etiquetas correspondientes a las frases

# Agrupamos las generaciones de frases en funciones 
# para mayor claridad

# Generación de alarmas con solo horas y minutos
def generar_alarmas_horas_minutos(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma a las %",
        simbolos=["%"],
        listas=[hora],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma a las % y $",
        simbolos=["%", "$"],
        listas=[hora, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[6, 59], #354
        #debug=True,
    )

    generar(
        plantilla="ponme una alarma a las % $",
        simbolos=["%", "$"],
        listas=[hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[18, 41], #738
        #debug=True,
    )

    generar(
        plantilla="ponme una alarma a las % horas y $ minutos",
        simbolos=["%", "$"],
        listas=[hora, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[8, 30], #240
        #debug=True,
    )

    generar(
        plantilla="ponme una alarma a las % y $",
        simbolos=["%", "$"],
        listas=[hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        #debug=True,
    )

    generar(
        plantilla="ponme una alarma a las % $",
        simbolos=["%", "$"],
        listas=[hora, min_menos],
        ds=dataset,
        dsbio=tags,
        #debug=True,
    )

    generar(
        plantilla="avísame a las % cero $",
        simbolos=["%", "$"],
        listas=[hora_24, min_cero],
        ds=dataset,
        dsbio=tags,
        #debug=True,
    )

def generar_alarmas_dia_concreto(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma el @ a las % y &",
        simbolos=["@", "%", "&"],
        listas=[dias, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7, 8, 20], #1120
        #debug=True
    )

    generar(
        plantilla="ponme una alarma el @ a las % y &",
        simbolos=["@", "%", "&"],
        listas=[dias, hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7, 10, 3], #210
        #debug=True
    )

    generar(
        plantilla="ponme una alarma el @ a las % &",
        simbolos=["@", "%", "&"],
        listas=[dias, hora, min_menos],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7, 6, 5], #210
        #debug=True
    )

    generar(
        plantilla="ponme una alarma cada @ a las % y $",
        simbolos=["@", "%", "$", "&"],
        listas=[dias, hora, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[4,6,20], #480
        #debug=True
    )

def generar_alarmas_mtn(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma a las % de la &",
        simbolos=["%", "&"],
        listas=[hora, mtn],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma el @ a las % y $ de la &",
        simbolos=["@", "%", "$", "&"],
        listas=[dias, hora, min_reloj, mtn],
        ds=dataset,
        dsbio=tags,
        subgrupos=[4,7,3,3], #252
        #debug=True
    )

    generar(
        plantilla="ponme una alarma a las % $ de la &",
        simbolos=["%", "$", "&"],
        listas=[hora, min_menos, mtn],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

def generar_alarmas_dias_combinados(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma los @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[20,4,10], #800
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma los @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias, hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        subgrupos=[40,4,3], #480
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma los @ a las % $",
        simbolos=["@", "%", "$"],
        listas=[dias, hora, min_menos],
        ds=dataset,
        dsbio=tags,
        subgrupos=[25,6,5], #750
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias_los, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[9,5,11], #495
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias_los, hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        subgrupos=[11,4,3], #132
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma @ a las % $",
        simbolos=["@", "%", "$"],
        listas=[dias_los, hora, min_menos],
        ds=dataset,
        dsbio=tags,
        subgrupos=[13,3,4], #156
        comb=[True, False, False],
        #debug=True
    )

def generar_alarmas_dias_relativos(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias_rel, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7,9,22], #1386
        comb=[True, False, False],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias_rel, hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        #subgrupos=[7,9,22], #252
        comb=[True, False, False],
        #debug=True
    )
    generar(
        plantilla="ponme una alarma @ a las % y $",
        simbolos=["@", "%", "$"],
        listas=[dias_rel, hora, min_menos],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7,5,5], #175
        comb=[True, False, False],
        #debug=True
    )

def generar_temporizadores(dataset:list, tags:list):
    generar(
        plantilla="ponme una alarma en % horas",
        simbolos=["%"],
        listas=[hora_24_rel],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % minutos",
        simbolos=["%"],
        listas=[min_rel],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % segundos",
        simbolos=["%"],
        listas=[seg_rel],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % horas y & minutos",
        simbolos=["%", "&"],
        listas=[hora_24_rel, min_rel],
        ds=dataset,
        dsbio=tags,
        subgrupos=[12, 30],
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % horas y &",
        simbolos=["%", "&"],
        listas=[hora_24_rel, min_reloj_rel],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % minutos y & segundos",
        simbolos=["%", "&"],
        listas=[min_rel, seg_rel],
        ds=dataset,
        dsbio=tags,
        subgrupos=[30, 30],
        #debug=True
    )

    # TODO: AVISAME?
    generar(
        plantilla="avísame en %",
        simbolos=["%"],
        listas=[min_rel],
        ds=dataset,
        dsbio=tags,
        #subgrupos=[30, 30],
        #debug=True
    )

    generar(
        plantilla="avísame en % horas $ minutos y & segundos",
        simbolos=["%", "$", "&"],
        listas=[hora_24_rel, min_rel, seg_rel],
        ds=dataset,
        dsbio=tags,
        subgrupos=[10, 24, 17], #4080
        #debug=True
    )

    generar(
        plantilla="avísame en % hora y $ minutos",
        simbolos=["%", "$", "&"],
        listas=[{"vals": ["una"], "bio": ["B-RELH"]}, min_rel],
        ds=dataset,
        dsbio=tags,
        debug=True
    )

    generar(
        plantilla="avísame en % hora y $",
        simbolos=["%", "$", "&"],
        listas=[{"vals": ["una"], "bio": ["B-RELH"]}, min_rel],
        ds=dataset,
        dsbio=tags,
        debug=True
    )

    generar(
        plantilla="avísame en % hora y $",
        simbolos=["%", "$", "&"],
        listas=[{"vals": ["una"], "bio": ["B-RELH"]}, min_reloj_rel],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

def generar_temporizadores_reloj(dataset:list, tags:list):
    generar(
        plantilla="avísame en % &",
        simbolos=["%", "&"],
        listas=[temp_reloj, relleno],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="ponme una alarma en % &",
        simbolos=["%", "&"],
        listas=[temp_reloj, relleno],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="dame un toque en % &",
        simbolos=["%", "&"],
        listas=[temp_reloj, relleno],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

def modificar_alarma(dataset:list, tags:list):
    generar(
        plantilla="cambia la alarma de las % y & a las $ y @",
        simbolos=["%", "&", "$", "@"],
        listas=[hora_24, min, hora_24_act, min_act],
        ds=dataset,
        dsbio=tags,
        subgrupos=[5,9,11,31],
        #debug=True
    )

    generar(
        plantilla="traspasa la alarma de las % y & a las $ y @",
        simbolos=["%", "&", "$", "@"],
        listas=[hora_24, min, hora_24_act, min_act],
        ds=dataset,
        dsbio=tags,
        subgrupos=[3,5,5,19],
        #debug=True
    )

    generar(
        plantilla="mueve la alarma de las % y & a las $ y @",
        simbolos=["%", "&", "$", "@"],
        listas=[hora_24, min, hora_24_act, min_act],
        ds=dataset,
        dsbio=tags,
        subgrupos=[3,5,4,8],
        #debug=True
    )

    generar(
        plantilla="cambia la alarma de las % a las $ y @",
        simbolos=["%", "$", "@"],
        listas=[hora_24, hora_24_act, min_act],
        ds=dataset,
        dsbio=tags,
        subgrupos=[5,9,17],
        #debug=True
    )

    generar(
        plantilla="actualiza la alarma de las % y & a las $",
        simbolos=["%", "&", "$"],
        listas=[hora_24, min, hora_24_act],
        ds=dataset,
        dsbio=tags,
        subgrupos=[3,15,10],
        #debug=True
    )

    generar(
        plantilla="actualiza la alarma de las % y @ a las & $",
        simbolos=["%", "@", "&", "$"],
        listas=[hora, min_reloj_act, hora_act, min_menos_act],
        ds=dataset,
        dsbio=tags,
        #debug=True
    )

    generar(
        plantilla="traspasa la alarma de las % de la @ a las & y $ de la ?",
        simbolos=["%", "@", "&", "$", "?"],
        listas=[hora, mtn, hora_act, min_act, mtn],
        ds=dataset,
        dsbio=tags,
        subgrupos=[5,3,7,3,3] # 945
        #debug=True
    )

def accion_alarma(dataset:list, tags:list):
    generar(
        plantilla="oye $ la alarma de las % y & ",
        simbolos=["$", "%", "&"],
        listas=[acciones, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[18,6,13], #1404
        #debug=True
    )

    generar(
        plantilla="me gustaría $ la alarma de las % y & ",
        simbolos=["$", "%", "&"],
        listas=[acciones, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[7,3,20], #420
        #debug=True
    )

    

    generar(
        plantilla="$ la alarma de las % & ",
        simbolos=["$", "%", "&"],
        listas=[acciones, hora, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[11,5,14], # 770
        #debug=True
    )

    generar(
        plantilla="quiero $ la alarma de las % y & ",
        simbolos=["$", "%", "&"],
        listas=[acciones_inf, hora, min_reloj],
        ds=dataset,
        dsbio=tags,
        subgrupos=[13,7,3], # 273
        #debug=True
    )

    generar(
        plantilla="$ la alarma de las % & ",
        simbolos=["$", "%", "&"],
        listas=[acciones_inf, hora, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[11,5,14], # 770
        #debug=True
    )

    generar(
        plantilla="por favor $ la alarma de las % y & ",
        simbolos=["$", "%", "&"],
        listas=[acciones, hora_24, min],
        ds=dataset,
        dsbio=tags,
        subgrupos=[10,4,15], #600
        #debug=True
    )

    generar(
        plantilla="quiero $ la alarma de las %",
        simbolos=["$", "%"],
        listas=[acciones_inf, hora_24],
        ds=dataset,
        dsbio=tags,
        subgrupos=[9,17], # 153
        #debug=True
    )

# Generamos el contenido
generar_alarmas_horas_minutos(datos, etiquetas)
generar_temporizadores(datos, etiquetas)
generar_temporizadores_reloj(datos, etiquetas)
generar_alarmas_dia_concreto(datos, etiquetas)
generar_alarmas_mtn(datos, etiquetas)
generar_temporizadores_reloj(datos, etiquetas)
generar_alarmas_dias_combinados(datos, etiquetas)
generar_alarmas_dias_relativos(datos, etiquetas)
generar_temporizadores_reloj(datos, etiquetas)
modificar_alarma(datos, etiquetas)
accion_alarma(datos, etiquetas)



pp(f"Frases generadas: {len(datos)}")
pp(f"Etiquetas generadas: {len(etiquetas)}")

'''pp("ERRORES EN ETIQUETACIÓN:")
for palabras, etiquetas in zip(datos, etiquetas):
    if(len(palabras) != len(etiquetas)):
        pp(palabras)
        pp(etiquetas)'''


# Volcamos resultados en un fichero de texto
try:
    with open("dataset.py", "w", newline='\n') as f:
        f.write(f"datos = {{\n\"comandos\":\n{str(datos)},\n\n\"tokens\": {str(etiquetas)}\n}}")

    print("Frases generadas volcadas con éxito en el fichero")

except:
    print("Error al volcar los datos")