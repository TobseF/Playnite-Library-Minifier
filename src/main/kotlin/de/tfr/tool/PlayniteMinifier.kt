package de.tfr.tool

import java.io.File

val sizeCover = Resolution(180, 270)
val sizeIcon = Resolution(550, 48)
val sizeBackground = Resolution(1920, 620)

const val simulate = true

const val libraryPath = "C:\\Users\\tobse\\AppData\\Roaming\\Playnite\\library\\"
const val gamesPath = libraryPath + "games\\"

const val outputPath = "C:\\Users\\tobse\\Desktop\\GameLibHtml\\"
const val imageMagicPath = "C:\\Program Files\\ImageMagick-7.1.0-Q16-HDRI\\magick.exe"
const val jpgImageQuality = 100

val games = readGames()

val duplicatedCoversMap: Map<String?, List<String?>> = mapGamesBy(Game::coverName)
val duplicatedIconsMap = mapGamesBy(Game::iconName)
val duplicatedBackGroundsMap = mapGamesBy(Game::backGroundName)
val duplicatedImagesMap = duplicatedCoversMap + duplicatedIconsMap + duplicatedBackGroundsMap

fun main() {
    resizeAllImages()
    fixHtmlPaths()
    deleteDuplicates()
}

private fun deleteDuplicates() {
    val allImages = games.flatMap { listOf(it.cover, it.backGround, it.backGround) }.filterNotNull()
    allImages.forEach {
        File(outputPath + it).deleteDuplicate()
        File(outputPath + it.replace("ico", "png")).deleteDuplicate()
        File(outputPath + it.replace("png", "jpg")).deleteDuplicate()
    }
}

fun String.replaceDuplicates() = this.replaceDuplicates(duplicatedImagesMap)

fun File.deleteDuplicate() {
    if (this.exists() && this.path.isDuplicates()) {
        if (!simulate) {
            this.delete()
        }
        println("Deleting duplicate: $this")
    }
}

fun String.isDuplicates(): Boolean  = this.isDuplicates(duplicatedImagesMap)

fun String.isDuplicates(map: Map<String?, List<String?>>): Boolean {
    // Image at index 0 is primary image
    return map.flatMap { it.value.drop(1) }.map { it?.replace("/", "\\") }.any { this.contains(it!!) }
}

fun String.replaceDuplicates(map: Map<String?, List<String?>>): String {
    var string = this
    map.forEach {
        val primaryImage = it.value.first()!!
        it.value.drop(1).forEach { image ->
            string = string.replace(image!!, primaryImage)
        }
    }
    return string
}

private fun mapGamesBy(ref: (Game) -> String?) = games.groupBy { it.name }.mapValues { it.value.map(ref) }
    .filter { it.value.size > 1 }

private fun fixHtmlPaths() {
    val website = File(outputPath).walkTopDown().maxDepth(1).filter(isType("html")).toList()
    val backGrounds = getGameImage(Game::backGround, "png")
    val covers = getGameImage(Game::cover, "png")
    website.forEachIndexed() { index, file ->
        println("Fixing Html ${index}/${website.size}")
        var content = file.readText()
        content = fixFileExtensions(content, backGrounds, covers)
        content = content.replaceDuplicates()
        file.writeText(content)
    }
}

private fun getGameImage(field: (Game) -> String?, fileType: String) =
    games.map(field).filterNotNull().filter { it.endsWith(".$fileType") }.map { it.substringAfterLast("\\") }.toSet()

private fun fixFileExtensions(content: String, backGrounds: Set<String>, covers: Set<String>): String {
    var fixes = content.replace("ico", "png")
    val allPngs = Regex("""[\w,-]*.png""")
    allPngs.findAll(fixes).forEach {
        val image = it.value
        if (backGrounds.contains(image) || covers.contains(image)) {
            fixes = fixes.replace(image, image.replace(".png", ".jpg"))
        }
    }
    return fixes
}

private fun resizeAllImages() {
    println("Scanning ${games.size} games")
    val images = games.sumOf { it.imageCount() }
    println("Resizing $images images")

    games.forEachIndexed() { index, game ->
        println("Converting Game: $game")
        game.backGround.resizeGameImage(sizeBackground, "jpg")
        game.cover.resizeGameImage(sizeCover, "jpg")
        game.icon.resizeGameImage(sizeIcon, "png")
        println("$index/${games.size}")
    }
}

private fun readGames(): List<Game> {
    return File(gamesPath).walkTopDown().maxDepth(1).filter(isType("json"))
        .map { Game.readGame(it) }.filter { !it.hidden }.toList()
}

fun String?.resizeGameImage(resolution: Resolution, fileType: String) {
    if (this != null) {
        resizeImage(outputPath + replace("\\\\", "\\"), resolution, fileType)
    }
}

private fun isType(extension: String): (File) -> Boolean = { it.path.endsWith(".$extension") }

data class Game(
    val gameId: String?, val name: String?, val backGround: String?,
    val icon: String?, val cover: String?, val hidden: Boolean = false
) {
    fun imageCount() = listOf(backGround, icon, cover).map { if (it == null ) 0 else 1 }.sum()

    companion object {
        fun readGame(file: File) = readGame(file.name.substringBefore(".json"), file.readLines().first())
        private fun readGame(gameId: String, gameData: String) =
            Game(
                gameId,
                gameData.readJsonValue("Name"),
                gameData.readJsonValue("BackgroundImage"),
                gameData.readJsonValue("Icon"),
                gameData.readJsonValue("CoverImage"),
                gameData.readJsonBoolean("Hidden")
            )
    }

    private fun String?.fileWithoutExtension() = this?.substringBeforeLast(".")?.replace("\\\\", "/")

    fun iconName() = icon.fileWithoutExtension()
    fun coverName() = cover.fileWithoutExtension()
    fun backGroundName() = backGround.fileWithoutExtension()
}

fun String.readJsonValue(name: String, suffix: String = ""): String? {
    return if (this.contains(name)) {
        this.substringAfterLast("""$suffix"$name":"""").substringBefore("\"")
    } else {
        null
    }
}

fun String.readJsonBoolean(name: String): Boolean {
    return this.substringAfter("$name\":").substringBefore(",") == "true"
}

data class Resolution(val width: Int, val height: Int)

fun String.icon(index: Int) = this.substringBeforeLast(".") + "-$index.png"

private fun resizeImage(sourceFile: String, destinationFile: String, resolution: Resolution) {
    val optResize = "-resize ${resolution.width}x${resolution.height}"
    convertImage(sourceFile, destinationFile, "-interlace plane -quality $jpgImageQuality $optResize");
}

private fun convertImage(sourceFile: String, destinationFile: String, options: String = "") {
    Runtime.getRuntime().exec("$imageMagicPath convert $sourceFile $options $destinationFile")
        .waitFor()
}

fun resizeImage(cover: String, resolution: Resolution, fileType: String) {
    var dest = cover.substringAfterLast("") + ".$fileType"
    val destinationType = cover.substringAfterLast(".")
    if (File(cover).exists()) {
        if (simulate) {
            println("Converting: $cover")
        } else {
            if (destinationType == "ico") {
                resizeIcon(cover, dest, resolution)
            } else {
                resizeImage(cover, dest, resolution)
            }
            if (!cover.endsWith(".$fileType")) {
                if (simulate) {
                    println("Deleting converted image: $cover")
                } else {
                    File(cover).delete()
                }
            }
        }
    } else {
        println("Missing file: $cover")
    }
}

/**
 * Resizes an .icon file. This is done by 1. splitting it into separate files. 2. Select the biggest one. 3. Convert the biggest the split files
 */
private fun resizeIcon(cover: String, dest: String, resolution: Resolution) {
    convertImage(cover, dest)
    val icons =
        (0..20).map { File(dest.icon(it)) }.filter { it.exists() }.sortedByDescending { it.length() }
    val bestIcon: File? = icons.firstOrNull()
    if (bestIcon != null) {
        resizeImage(bestIcon.path, dest, resolution)
        icons.forEach { it.delete() }
    } else {
        resizeImage(cover, dest, resolution)
    }
}

