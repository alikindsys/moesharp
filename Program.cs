using System.Net;
using System.IO;
using System.Text.RegularExpressions;
using System.Linq;
using System;
using OpenQA.Selenium;
using SeleniumExtras.WaitHelpers;
using OpenQA.Selenium.Chrome;
using OpenQA.Selenium.Support.UI;
using OpenQA.Selenium.Remote;
using Newtonsoft.Json;
using System.Diagnostics;

namespace moesharp
{
    //Dowloads animes from twist.moe 
    //TODO: Fetching data from website using selenium. Download stuff via default downloader.
    // PS : used aria2c with python so there is a requirement less.
    // PPS : cross compatibility.
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Count() == 0)
            {
                Console.WriteLine($"Please type the link or the name");
                Environment.Exit(-1);
            }
            else
            {
                string line = "";
                args.ToList().ForEach(x => line += $"{x} ");
                Regex isLink = new Regex(@"(http|https)\:\/\/\S*\/*\S*");
                var matches = isLink.Matches(line).ToList();
                if (matches.Count > 0)
                {
                    Console.WriteLine($"Link(s) detected # : {matches.Count}");
                    foreach (var link in matches)
                    {
                        DownloadSeries(link.ToString());
                    }
                }
                else
                {
                    Console.WriteLine("Search will be added in the future.");
                    //TODO
                }

            }
        }



        private static void DownloadSeries(string v)
        {
            if (v.StartsWith("https://twist.moe/"))
            {
                var cplp = new ChromePerformanceLoggingPreferences();
                var opt = new ChromeOptions();
                opt.AddArgument("--headless");
                using (var driver = new ChromeDriver(Directory.GetCurrentDirectory(), options: opt, commandTimeout: TimeSpan.FromMinutes(10)))
                {
                    driver.Url = v;
                    var wait = new WebDriverWait(driver, TimeSpan.FromMinutes(10));
                    var site = wait.Until(ExpectedConditions.ElementExists(By.TagName("title")));
                    var title = site.GetAttribute("text");
                    var episodeCount = driver.FindElementsByClassName("episode-number").ToList().Count();
                    var video = driver.FindElementByTagName("video");
                    var src = video.GetAttribute("src");
                    // Console.WriteLine($"Logs Found : {logs.Count}");
                    Regex animeName = new Regex(@"(.\(.+)|(.Episode.+)");
                    string SeriesName = animeName.Replace(title, "").ToString();
                    Console.WriteLine($"Found Anime : {SeriesName}");
                    Console.WriteLine($"Episodes : {episodeCount}");
                    Console.WriteLine($"\tChecking src logic...");
                    Regex findNum = new Regex(@"\d+");
                    var b = "";
                    if (src.Contains("%20"))
                    {
                        b = src.Replace("%20", " ");
                    }
                    else
                    {
                        b = src;
                    }
                    src = b;
                    var numsearch = findNum.Match(src);
                    var linkWithoutNum = src.Remove(numsearch.Index, numsearch.Length);
                    //AAAAAAAAAAAA-01.mp4 [12] -> AAAAAAAAAAAA-.mp4 [12] gg
                    var formatedNum = episodeCount.ToString().PadLeft(numsearch.Length, '0');
                    var lastEpLink = linkWithoutNum.Insert(numsearch.Index, formatedNum);
                    Uri uriResult;
                    driver.Dispose();
                    bool result = Uri.TryCreate(lastEpLink, UriKind.Absolute, out uriResult)
                        && (uriResult.Scheme == Uri.UriSchemeHttp || uriResult.Scheme == Uri.UriSchemeHttps);
                    if (result)
                    {
                        Console.WriteLine("\tLogic Decoded. Download will start soon.");
                        for (int i = 1; i < episodeCount + 1; i++)
                        {
                            formatedNum = i.ToString().PadLeft(numsearch.Length, '0');
                            var EpLink = linkWithoutNum.Insert(numsearch.Index, formatedNum);
                            Console.WriteLine($"[{SeriesName}] Downloading EP {i} to /{SeriesName}.");
                            if (!Directory.Exists(Directory.GetCurrentDirectory() + $"/{SeriesName}"))
                            {
                                Directory.CreateDirectory($"{Directory.GetCurrentDirectory()}/{SeriesName}");
                            }
                            using (WebClient wc = new WebClient())
                            {
                                var request = GetRequest(v);
                                HttpWebRequest get = (HttpWebRequest)HttpWebRequest.Create(EpLink);
                                get.CookieContainer = new CookieContainer();
                                get.CookieContainer.Add(new System.Net.Cookie
                                {
                                    Name = request.Response.Cookies[0].Name,
                                    Value = request.Response.Cookies[0].Value,
                                    Comment = request.Response.Cookies[0].Comment,
                                    Domain = request.Response.Cookies[0].Domain
                                });
                                get.Headers = new WebHeaderCollection();
                                foreach (var header in request.Request.Headers)
                                {
                                    get.Headers.Add(header.Name, header.Value);
                                }
                                get.Method = WebRequestMethods.Http.Get;
                                ServicePointManager
                                .ServerCertificateValidationCallback +=
                                (sender, cert, chain, sslPolicyErrors) => true;
                                HttpWebResponse response = (HttpWebResponse)get.GetResponse();
                                Stream httpResponseStream = response.GetResponseStream();
                                int bufferSize = 1024;
                                byte[] buffer = new byte[bufferSize];
                                int bytesRead = 0;
                                FileStream fileStream = File.Create($"{Directory.GetCurrentDirectory()}/{SeriesName}/EP{i}.mp4");
                                while ((bytesRead = httpResponseStream.Read(buffer, 0, bufferSize)) != 0)
                                {
                                    fileStream.Write(buffer, 0, bytesRead);
                                }
                                Console.WriteLine($"Done downloading EP {i} from {SeriesName}");
                            }
                        }
                        foreach (var process in Process.GetProcessesByName("java"))
                        {
                            process.Kill();
                        }
                        Console.WriteLine("[pKill] Killed java instances (used on the cookie grabbing)");
                    }

                }
            }
            else
            {
                Console.WriteLine($"Website {v} not supported.");
            }

        }

        private static Entry GetRequest(string redirect)
        {
            CallPy(redirect);
            Har r = JsonConvert.DeserializeObject<Har>(File.ReadAllText($"{Directory.GetCurrentDirectory()}/har.json"));
            var cookie = r.Log.Entries.Find(x => x.Response.Cookies.Any(y => y.Name == "__cfduid"));
            return cookie;
        }

        private static void CallPy(string redirect)
        {
            Console.WriteLine("[Calling Python Script in order to get a valid cookie]");
            CmdPy("grab.py", redirect);

            Console.WriteLine("[Cookie grabbed.]");

        }
        public static void CmdPy(string cmd, string args)
        {
            ProcessStartInfo start = new ProcessStartInfo();
            start.FileName = "python";
            start.Arguments = string.Format("\"{0}\" \"{1}\"", cmd, args);
            start.UseShellExecute = false;// Do not use OS shell
            start.CreateNoWindow = true; // We don't need new window
            start.RedirectStandardOutput = true;// Any output, generated by application will be redirected back
            start.RedirectStandardError = true; // Any error in standard output will be redirected back (for example exceptions)
            var p = Process.Start(start);
            p.WaitForExit();
        }
    }
}
