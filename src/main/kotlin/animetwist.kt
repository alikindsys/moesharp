import com.sun.jndi.toolkit.url.Uri
import okhttp3.internal.closeQuietly
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class MoeAnime(val link: Uri, val episodeCount: Int, val name: String) {
    constructor(link: String, driver: WebDriver) :
            this(
                Uri(link.processLink()),
                driver.getAnimeSize(link),
                driver.getAnimeName(link)
            )
}

data class MoeAnimeEpisode(val anime: MoeAnime, val episodeNumber: Int, val url: String)

internal fun WebDriver.getEpisodes(anime: MoeAnime) : List<MoeAnimeEpisode> {
    val episodes: MutableList<MoeAnimeEpisode> = mutableListOf()
    println("Getting episodes for : [${anime.name}]")
    for( i in 1..anime.episodeCount){
        println("Current episode : $i")
        this.get("${anime.link}/$i")
        var elem = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/div/div/video"))
        var src = elem.getAttribute("src")
        while(src.isBlank()){
            elem = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/div/div/video"))
            src = elem.getAttribute("src")
        }

        episodes.add(
            MoeAnimeEpisode(
                anime, i, decodeURL(
                    src.replace(
                        "https://twistcdn.bunny.sh/anime/",
                        "${getKnownCDNServer()}/anime/"
                    )
                )
            )
        )
    }
    println("Finished getting episodes for ${anime.name}. Count : ${episodes.count()}")
    return episodes.toList()
}

internal fun WebDriver.getAnimeSize(link: String) : Int {
    this.get(link)
    val e = this.findElement(By.ByXPath("/html/body/div[1]/div[2]/div/div[1]/section/main/div[2]/div[3]"))
    val episodes = e.findElements(By.tagName("li"))
    return episodes.count()
}

internal fun WebDriver.getAnimeName(link: String) : String {
    this.get(link)
    val e = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/main/div[2]/div[1]/h2/span"))
    return e.text.trim()
}
internal fun String.processLink() : String {
    val processedLink : MutableList<String> = mutableListOf()
    for (s in this.split("/")) {
        if(s.contains(Regex("\\d+"))) continue
        processedLink.add(s)
    }
    return processedLink.joinToString(separator = "/")
}

internal fun String.isAnimeTwist() : Boolean {
    return this.contains("https://twist.moe/a/")
}

internal fun WebDriver.getAllAnimes(args:Array<String>) : List<MoeAnime> {
    val tempAnimes = mutableListOf<MoeAnime>()
    for(s in args){
        if(!s.isAnimeTwist()){
            println("[Invalid Link] $s wasn't recognized as a valid animetwist link. Skipped.")
            continue
        }
        val tempAnime = MoeAnime(s,this)
        tempAnimes.add(tempAnime)
        println("[${tempAnime.name}] was detected successfully!")
    }
    return tempAnimes.toList()
}

internal fun MoeAnimeEpisode.download(config: Config) {
    val file = File("${config.animepath}/${this.anime.name}/${this.episodeNumber}.mp4")
    val path = File("${config.animepath}/${this.anime.name}")
    println("[${this.episodeNumber}/${this.anime.episodeCount}] Downloading ${this.anime.name} - Episode ${this.episodeNumber} to [${file.canonicalPath}]")
    path.mkdirs()
    if(!file.exists()) file.createNewFile()
    val url = URL(this.url)
    val len = this.getLength(url)
    if(file.length() == len){
        println("[SKIPPED] Episode ${this.episodeNumber} of [${this.anime.name}]")
        println("Already downloaded.")
        return
    }
    val request = this.getInputStreamRequest(url, file, len)
    if(file.length() != 0L) println("[${this.anime.name}] Resuming download of [Episode ${this.episodeNumber}] ${file.length()}/$len")
    if(request == null) this.download(config)
    else {
        request.inputStream.writeAllToFile(file)
        request.inputStream.closeQuietly()
        request.disconnect()
    }
    if(file.length() != len){
        println("[Remote Server] Connection closed unexpectedly. Reconnecting")
        this.download(config)
    } else {
        println("Download Completed.")
    }
}

internal fun decodeURL(str: String) : String {
    return str.replace("%2520", " ").replace("%5B", "[").replace("%5D", "]")
}

internal fun getKnownCDNServer() : String {
    val rnd = Random.nextInt(1, 60)
    return "https://edge-$rnd.cdn.bunny.sh"
}

internal fun InputStream.writeAllToFile(file : File) {
    val fos = FileOutputStream(file, true)
    val buffer = ByteArray(100 * 1024)
    var bytesRead: Int
    while (this.read(buffer).also { bytesRead = it } != -1) {
        fos.write(buffer, 0, bytesRead)
    }
    fos.closeQuietly()
}

internal fun MoeAnimeEpisode.getLength(url : URL) : Long {
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "HEAD"
    conn.setRequestProperty("referer", "${this.anime.link}/${this.episodeNumber}")
    val size = conn.contentLengthLong
    conn.disconnect()
    return size
}

internal fun MoeAnimeEpisode.getInputStreamRequest(url : URL, file: File, length : Long) : HttpURLConnection? {
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.setRequestProperty("referer", "${this.anime.link}/${this.episodeNumber}")
    conn.setRequestProperty("Connection", "keep-alive")
    conn.setRequestProperty("accept-encoding", "identity")
    conn.setRequestProperty("Range", "bytes=${file.length()}-$length")
    val code = conn.responseCode
    if(code != 206 && code != 200) {
        println("=============================[ERROR]=============================")
        println("An unexpected status code was returned. Please report this!!!")
        println("Status Code : $code")
        println("Raised from : ${conn.url}")
        println("Report HERE : https://github.com/RORIdev/moesharp/issues/3")
        println("=============================[ERROR]=============================")
        println("moetk will now reload. This could fix the issue.")
        println("")
        println("")
        return null
    }
    return conn
}