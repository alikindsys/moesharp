import com.google.gson.Gson
import io.netty.handler.logging.LogLevel
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverLogLevel
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxDriverLogLevel
import org.openqa.selenium.firefox.FirefoxOptions
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class Config(val browser : String, val driverpath: String, val animepath: String) {
    constructor(browser: String) : this(browser, "", "")
    constructor(browser: String, driverpath: String) : this (browser, driverpath, "")
}
fun main(args: Array<String>) {
    val driver : WebDriver
    val file = File("./config.json")
    val cfg = createConfig(file)
    if(args.isEmpty()){
        println("Usage : moetk [link] <link2> ...")
        println("Please add at least ONE link.")
        exitProcess(-1)
    }
    System.setProperty("webdriver.chrome.driver",cfg.driverpath)
    System.setProperty("webdriver.gecko.driver",cfg.driverpath)
    driver = when (cfg.browser) {
        "firefox" -> FirefoxDriver(FirefoxOptions().setHeadless(true).setLogLevel(FirefoxDriverLogLevel.WARN))
        "chrome" -> ChromeDriver(ChromeOptions().setHeadless(true).setLogLevel(ChromeDriverLogLevel.WARNING))
        else -> exitProcess(-1)
    }

    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    val animes = driver.getAllAnimes(args)
    val episodes = animes.flatMap {driver.getEpisodes(it)}
    driver.quit()
    for(episode in episodes) {
        episode.download(cfg)
    }
    exitProcess(0)
}

fun createConfig(file: File) : Config {
    var cfg : Config
    if(file.exists()){
        cfg = Gson().fromJson<Config>(file.readText(), Config::class.java)
    } else {
        println("Please select the browser you prefer:")
        println("1. Firefox")
        println("2. Chrome")
        val input = readLine()?.trim()
        if(input != null){
            cfg = when {
                input[0] == '1' -> {
                    Config("firefox")
                }
                input[0] == '2' -> {
                    Config("chrome")
                }
                else -> {
                    println("Invalid option. Quitting")
                    exitProcess(-1)
                }
            }
        } else {
            println("Invalid option. Quitting")
            exitProcess(-1)
        }
        println("Please input the path to ${if (cfg.browser == "firefox") "gecko" else cfg.browser}driver")
        println("Please download the version that matches your current ${cfg.browser} version")
        val input2 = readLine()?.trim()
        if(input2 != null) {
            val check = File(input2)
            if(!check.exists()){
                println("Invalid path. Quitting")
                exitProcess(-1)
            }
            cfg = Config(cfg.browser, check.canonicalPath)
        }
        println("Please input the path of the anime folder")
        println("\"anime folder\" - where you want anime to be downloaded to.")
        val input3 = readLine()?.trim()
        if(input3 != null) {
            val check = File(input3)
            check.mkdirs()
            cfg = Config(cfg.browser, cfg.driverpath, check.canonicalPath)
        }
        file.writeText(Gson().toJson(cfg))
    }
    return cfg
}