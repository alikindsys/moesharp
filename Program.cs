using System.Net;
using System.IO;
using System.Text.RegularExpressions;
using System.Linq;
using System;
using OpenQA.Selenium;
using SeleniumExtras.WaitHelpers;
using OpenQA.Selenium.Chrome;
using OpenQA.Selenium.Support.UI;

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
                                wc.DownloadFile(EpLink, $"{Directory.GetCurrentDirectory()}/{SeriesName}/EP{i}.mp4");
                            }
                        }
                    }

                }
            }
            else
            {
                Console.WriteLine($"Website {v} not supported.");
            }

        }

    }
}
