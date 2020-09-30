import com.google.gson.Gson
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import java.io.File
import kotlin.system.exitProcess

data class Config(val browser : String, val path: String) {
    constructor(browser: String) : this(browser, "")
}
fun main(args: Array<String>) {
    val driver : WebDriver
    val file = File("./config.json")
    val cfg = createConfig(file)
    System.setProperty("webdriver.chrome.driver",cfg.path)
    System.setProperty("webdriver.gecko.driver",cfg.path)
    driver = when (cfg.browser) {
        "firefox" -> FirefoxDriver()
        "chrome" -> ChromeDriver()
        else -> exitProcess(-1)
    }
    try {
        driver.get("https://google.com")
    } finally {
        driver.quit()
    }
    println("Hello World!")
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
        file.writeText(Gson().toJson(cfg))
    }
    return cfg
}