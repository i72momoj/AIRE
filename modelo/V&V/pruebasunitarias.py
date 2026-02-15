
import sys
sys.path.insert(1, './V&V')
from validar import *

dirmodelo = "./onnx"

# --------------------------------------------------------------#
#                        Crear alarma 
#---------------------------------------------------------------#
pp("PRUEBAS CREAR ALARMA")
validar_resultados(
    text="ponme una alarma a las siete de la mañana",
    bio="O O O O O B-HORA O O B-MTN",
    dir=dirmodelo
)

validar_resultados(
    text="avísame a las ocho y cuarenta",
    bio="O O O B-HORA O B-MIN",
    dir=dirmodelo
)

validar_resultados(
    text="ponme una alarma mañana y pasado a las dos y media de la tarde",
    bio="O O O B-DSEM O B-DSEM O O B-HORA O B-MIN O O B-MTN",
    dir=dirmodelo
)

validar_resultados(
    text="pégame un toque a las cinco cero cinco",
    bio="O O O O O B-HORA O B-MIN",
    dir=dirmodelo
)

validar_resultados(
    text="avísame los lunes martes jueves y sábados a las siete menos cuarto",
    bio="O O B-DSEM B-DSEM B-DSEM O B-DSEM O O B-HORA B-MIN I-MIN",
    dir=dirmodelo
)

# --------------------------------------------------------------#
#                        Modificar alarma 
#---------------------------------------------------------------#
pp("PRUEBAS MODIFICAR ALARMA")
validar_resultados(
    text="cámbiame la alarma de las nueve de la mañana a las diez y cuarto",
    bio="O O O O O B-HORA O O B-MTN O O B-ACTH O B-ACTM",
    dir=dirmodelo
)

validar_resultados(
    text="traspasa el aviso de las siete a las nueve menos veinticinco de la mañana",
    bio="O O O O O B-HORA O O B-ACTH B-ACTM I-ACTM O O B-MTN",
    dir=dirmodelo
)

validar_resultados(
    text="cámbiame la alarma de las doce y media de la mañana a las cinco y treinta y cinco de la tarde",
    bio="O O O O O B-HORA O B-MIN O O B-MTN O O B-ACTH O B-ACTM I-ACTM I-ACTM O O B-MTN",
    dir=dirmodelo
)

# --------------------------------------------------------------#
#                        Eliminar alarma 
#---------------------------------------------------------------#
pp("PRUEBAS ELIMINAR ALARMA")
validar_resultados(
    text="quiero eliminar la alarma de las diecisiete y cuarenta y cinco",
    bio="O B-ACCION O O O O B-HORA O B-MIN I-MIN I-MIN",
    dir=dirmodelo
)

validar_resultados(
    text="borra el aviso de la una cero cinco",
    bio="B-ACCION O O O O B-HORA O B-MIN",
    dir=dirmodelo
)

validar_resultados(
    text="por favor suprime la alarma de las diecinueve treinta",
    bio="O O B-ACCION O O O O B-HORA B-MIN",
    dir=dirmodelo
)
# --------------------------------------------------------------#
#                        Activar alarma 
#---------------------------------------------------------------#
pp("PRUEBAS ACTIVAR ALARMA")
validar_resultados(
    text="activa la alarma de las siete y veinte de la mañana",
    bio="B-ACCION O O O O B-HORA O B-MIN O O B-MTN",
    dir=dirmodelo
)

validar_resultados(
    text="necesito que enciendas la alarma de las once",
    bio="O O B-ACCION O O O O B-HORA",
    dir=dirmodelo
)

validar_resultados(
    text="por favor activa el aviso de las cuatro y cuarto",
    bio="O O B-ACCION O O O O B-HORA O B-MIN",
    dir=dirmodelo
)
# --------------------------------------------------------------#
#                        Desactivar alarma 
#---------------------------------------------------------------#
pp("PRUEBAS DESACTIVAR ALARMA")
validar_resultados(
    text="apaga la alarma de las cuatro y media de la tarde por favor",
    bio="B-ACCION O O O O B-HORA O B-MIN O O B-MTN O O",
    dir=dirmodelo
)

validar_resultados(
    text="necesito que desactives el aviso de las doce",
    bio="O O B-ACCION O O O O B-HORA",
    dir=dirmodelo
)

validar_resultados(
    text="anula la alarma de las diecisiete cuarenta y dos",
    bio="B-ACCION O O O O B-HORA B-MIN I-MIN I-MIN",
    dir=dirmodelo
)

# --------------------------------------------------------------#
#                        Crear temporizador 
#---------------------------------------------------------------#
pp("PRUEBAS DESACTIVAR ALARMA")
validar_resultados(
    text="ponme un temporizador de quince minutos",
    bio="O O O O B-RELM O",
    dir=dirmodelo
)

validar_resultados(
    text="avísame en una hora y cuarto",
    bio="O O B-RELH O O B-RELM",
    dir=dirmodelo
)

validar_resultados(
    text="pégame un toque en dieciocho horas treinta y dos minutos y cuarenta y siete segundos",
    bio="O O O O B-RELH O B-RELM I-RELM I-RELM O O B-RELS I-RELS I-RELS O",
    dir=dirmodelo
)

validar_resultados(
    text="necesito que suenes en treinta y cinco minutos es importante",
    bio="O O O O B-RELM I-RELM I-RELM O O O",
    dir=dirmodelo
)