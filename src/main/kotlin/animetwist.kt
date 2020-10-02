import com.sun.jndi.toolkit.url.Uri
import okhttp3.internal.closeQuietly
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class MoeAnime(val link: Uri, val length: Int, val name: String) {
    constructor(link: String, driver: WebDriver) :
            this(
                Uri(link.processLink()),
                driver.getAnimeSize(link),
                driver.getAnimeName(link)
            )
}

data class MoeAnimeResource(val anime: MoeAnime, val episode: Int, val url: String)

fun WebDriver.getEpisodes(anime: MoeAnime) : List<MoeAnimeResource> {
    val resources: MutableList<MoeAnimeResource> = mutableListOf()
    println("Getting episodes for : [${anime.name}]")
    for( i in 1..anime.length){
        println("Current episode : $i")
        this.get("${anime.link}/$i")
        var elem = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/div/div/video"))
        var src = elem.getAttribute("src")
        while(src.isBlank()){
            elem = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/div/div/video"))
            src = elem.getAttribute("src")
        }

        resources.add(
            MoeAnimeResource(
                anime, i, decodeURL(
                    src.replace(
                        "https://twistcdn.bunny.sh/anime/",
                        "${getKnownCDNServer()}/anime/"
                    )
                )
            )
        )
    }
    println("Finished getting episodes for ${anime.name}. Count : ${resources.count()}")
    return resources.toList()
}

fun WebDriver.getAnimeSize(link: String) : Int {
    this.get(link)
    val e = this.findElement(By.ByXPath("/html/body/div[1]/div[2]/div/div[1]/section/main/div[2]/div[3]"))
    val episodes = e.findElements(By.tagName("li"))
    return episodes.count()
}

fun WebDriver.getAnimeName(link: String) : String {
    this.get(link)
    val e = this.findElement(By.xpath("/html/body/div[1]/div[2]/div/div[1]/section/main/div[2]/div[1]/h2/span"))
    return e.text.trim();
}
fun String.processLink() : String {
    val processedLink : MutableList<String> = mutableListOf()
    for (s in this.split("/")) {
        if(s.contains(Regex("\\d+"))) continue
        processedLink.add(s)
    }
    return processedLink.joinToString(separator = "/")
}

fun String.isAnimeTwist() : Boolean {
    return this.contains("https://twist.moe/a/")
}

fun WebDriver.getAllAnimes(args:Array<String>) : List<MoeAnime> {
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

fun MoeAnimeResource.download(config: Config) {
    val file = File("${config.animepath}/${this.anime.name}/${this.episode}.mp4")
    val path = File("${config.animepath}/${this.anime.name}")
    println("[${this.episode}/${this.anime.length}] Downloading ${this.anime.name} - Episode ${this.episode} to [${file.canonicalPath}]")
    path.mkdirs()
    val url = URL(this.url)
    val checkSizeConn = url.openConnection() as HttpURLConnection
    checkSizeConn.setRequestProperty("referer", "${this.anime.link}/${this.episode}")
    checkSizeConn.requestMethod = "HEAD"
    val len = checkSizeConn.contentLengthLong;
    checkSizeConn.disconnect()
    val getFileConn = url.openConnection() as HttpURLConnection
    getFileConn.requestMethod = "GET"
    getFileConn.setRequestProperty("referer", "${this.anime.link}/${this.episode}")
    getFileConn.setRequestProperty("Connection", "keep-alive")
    getFileConn.setRequestProperty("accept-encoding", "identity")

    if(!file.exists() || file.length() != len) {
        if(!file.exists()) file.createNewFile()
        getFileConn.setRequestProperty("Range", "bytes=${file.length()}-$len")
        if(file.length() != 0L) println("[${this.anime.name}] Resuming download of [Episode ${this.episode}] ${file.length()}/$len")
    }else if(file.length() == len){
        println("[SKIPPED] Episode ${this.episode} of [${this.anime.name}]")
        println("Already downloaded.")
        return
    }

    val code = getFileConn.responseCode
    if(code == 200 || code == 206){
        println("Download started.")
        val stream = getFileConn.inputStream
        stream.writeAllToFile(file)
        stream.closeQuietly()
        println("Download Ended.")
    } else {
        println("=============================[ERROR]=============================")
        println("An unexpected status code was returned. Please report this!!!")
        println("Status Code : $code")
        println("Raised from : ${getFileConn.url}")
        println("Report HERE : https://github.com/RORIdev/moesharp/issues/3")
        println("=============================[ERROR]=============================")
        println("moetk will now reload. This could fix the issue.")
        println("")
        println("")
        getFileConn.disconnect()
        this.download(config)
    }
    getFileConn.disconnect()
    if(file.length() != len){
        println("[Remote Server] Connection closed before expected bit.")
        this.download(config)
    }
}

fun decodeURL(str: String) : String {
    return str.replace("%2520", " ").replace("%5B", "[").replace("%5D", "]")
}

fun getKnownCDNServer() : String {
    val rnd = Random.nextInt(1, 60)
    return "https://edge-$rnd.cdn.bunny.sh"
}

fun InputStream.writeAllToFile(file : File) {
    val fos = FileOutputStream(file, true)
    val buffer = ByteArray(100 * 1024)
    var bytesRead: Int
    while (this.read(buffer).also { bytesRead = it } != -1) {
        fos.write(buffer, 0, bytesRead)
    }
    fos.closeQuietly()
}