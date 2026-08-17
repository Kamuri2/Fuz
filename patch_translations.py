import re

with open('app/src/main/java/com/example/ui/Translations.kt', 'r') as f:
    content = f.read()

# English
content = content.replace('"songs" to "songs"', '"songs" to "songs",\n        "about_artist" to "About the artist",\n        "followers" to "Followers",\n        "origin" to "Origin",\n        "no_info_available" to "No information available."')
# Spanish
content = content.replace('"songs" to "canciones"', '"songs" to "canciones",\n        "about_artist" to "Acerca del artista",\n        "followers" to "Seguidores",\n        "origin" to "Origen",\n        "no_info_available" to "No hay información disponible."')
# Portuguese
content = content.replace('"songs" to "músicas"', '"songs" to "músicas",\n        "about_artist" to "Sobre o artista",\n        "followers" to "Seguidores",\n        "origin" to "Origem",\n        "no_info_available" to "Nenhuma informação disponível."')
# French
content = content.replace('"songs" to "chansons"', '"songs" to "chansons",\n        "about_artist" to "À propos de l\'artiste",\n        "followers" to "Abonnés",\n        "origin" to "Origine",\n        "no_info_available" to "Aucune information disponible."')
# German
content = content.replace('"songs" to "Lieder"', '"songs" to "Lieder",\n        "about_artist" to "Über den Künstler",\n        "followers" to "Follower",\n        "origin" to "Herkunft",\n        "no_info_available" to "Keine Informationen verfügbar."')

with open('app/src/main/java/com/example/ui/Translations.kt', 'w') as f:
    f.write(content)
