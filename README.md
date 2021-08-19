# 🎮🗜 Playnite Library Minifier
[![Kotlin](https://img.shields.io/badge/Kotlin-1.5.10-blue.svg?style=flat&logo=kotlin&logoColor=white)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-16-red.svg?style=flat&logo=Java&logoColor=white)](https://adoptopenjdk.net)
[![Gradle](https://img.shields.io/badge/Gradle-7.1-08313A.svg?style=flat&logo=Java&logoColor=white)](https://gradle.org)

> 🗜 Shrink your game lib to make it ready for an upload

A one file Kotlin script which shrinks a [Playnite](https://playnite.link) game library by resizing images and removing duplicated files.
The script can also update paths in a [Playnite html exporter](https://github.com/joyrider3774/Playnite_html_exporter) export.

To resize the images it uses [Image Magic](https://imagemagick.org/index.php).

### 👉 Sample Game Lib: [games.tobse.eu](http://games.tobse.eu)
|      | Before  | After
|----- | --------|-------
|Size  | 4.42 GB | 466 MB
|Files |    8875 |   8210

## ⭐ Features
 ⭐ Save up to **90%** space   
 ⭐ Resizing images  
 ⭐ Converting .ico files to png  
 ⭐ Removing duplicates files  
 ⭐ Update static html files

## 🛠 Config
Before launch, edit the variables in the script according your machine setup.
 * `simulate`: For testing. If true, no files will be changed.
 * `gamesPath`: Your Playnite library game folder. Used to load the game data.
 * `outputPath`: Folder where it converts the images.
 * `imageMagicPath`: Path to the magick.exe.
 * `jpgImageQuality`: JPG image quality, 100 ist best.

## 🚀 Run
 1. At first install the Playnite html exporter plugin. 
 2. In the config check "Copy images to output folder".
 3. Make an export _"Extensions > HTML Exporter > Export"_
 4. Run the script by gradle

```kotlin
gradle run
```
