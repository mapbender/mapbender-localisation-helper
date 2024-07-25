# Mapbender Locale Exporter
PHPStorm extension to quickly localise strings.

Check wiki for an installable jar.

## Usage
- Select a hard-coded string in a PHP, JS or twig file.
- Press Ctrl-Numpad1 (can be changed in Settings) or Tools -> Extract Translation in the menu
- Enter a translation key (e.g. mb.actions.myaction) and all translations you know. The previously selected text will be prepopulated as the English text.
- Press Enter. The selected text will be replaced by the entered key, including `Mapbender.trans` in JS files and `{{ key | trans}}` in twig templates

## Building the plugin
- Call `./gradlew buildPlugin`
- The plugin will be saved in `build/libs` and can be installed via File - Settings - Plugin - Settings Icon - Install Plugin from Disk
