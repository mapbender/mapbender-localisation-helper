# Mapbender Locale Exporter
PHPStorm extension to quickly localise strings.

Installable Jar is available at https://files.wheregroup.com/s/63SxXCcLDq6rJJs

## Usage - Extract hardcoded string
- Select a hard-coded string in a PHP, JS or twig file.
- Press Ctrl-Numpad1 (can be changed in Settings) or `Tools -> Extract Translation` in the menu
- Enter a translation key (e.g. mb.actions.myaction) and all translations you know. The previously selected text will be prepopulated as the English text.
- Press Enter. The selected text will be replaced by the entered key, including `Mapbender.trans` in JS files and `{{ key | trans}}` in twig templates

## Usage - Modify existing translations
- Select an existing mapbender translation string in a PHP, JS or twig file.
- Press Ctrl-Numpad2 (can be changed in Settings) or `Tools -> Modify Translation` in the menu
- All existing translations to that key will be shown. You can modify the translation key and the individual translated strings for all languages
- You can automate translations by clicking the world icon next to the string you want to use as your translation source using a local installation of libretranslate (see below)
- Press Enter. All modified translations will be updated in the yaml-files


## Building the plugin
- Call `./gradlew buildPlugin`
- The plugin will be saved in `build/libs` and can be installed via File - Settings - Plugin - Settings Icon - Install Plugin from Disk

## Automated translations
The plugin supports automated translations using [LibreTranslate](https://github.com/LibreTranslate/LibreTranslate). You can only use a locally
installed version at the moment to avoid API Key problems.

Installation is very easy (python 3 required):

```bash
pip install libretranslate
libretranslate --load-only de,en,fr,es,pt,ru,uk,tr,it,nl --update-models # first-start
libretranslate --load-only de,en,fr,es,pt,ru,uk,tr,it,nl # subsequent starts
```

The service must run on port 500p0 (default). Now you can use the globe icon in the Modify translation dialog to automate translations.