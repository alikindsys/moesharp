namespace moesharp
{
    using System;
    using System.Collections.Generic;
    using Newtonsoft.Json;


    public partial class Har
    {
        [JsonProperty("log")]
        public Log Log { get; set; }
    }

    public partial class Log
    {
        [JsonProperty("version")]
        public string Version { get; set; }

        [JsonProperty("creator")]
        public Creator Creator { get; set; }

        [JsonProperty("pages")]
        public List<Page> Pages { get; set; }

        [JsonProperty("entries")]
        public List<Entry> Entries { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class Creator
    {
        [JsonProperty("name")]
        public string Name { get; set; }

        [JsonProperty("version")]
        public string Version { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class Entry
    {
        [JsonProperty("pageref")]
        public Uri Pageref { get; set; }

        [JsonProperty("startedDateTime")]
        public DateTimeOffset StartedDateTime { get; set; }

        [JsonProperty("request")]
        public Request Request { get; set; }

        [JsonProperty("response")]
        public Response Response { get; set; }

        [JsonProperty("cache")]
        public Cache Cache { get; set; }

        [JsonProperty("timings")]
        public Timings Timings { get; set; }

        [JsonProperty("serverIPAddress")]
        public string ServerIpAddress { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }

        [JsonProperty("time")]
        public long Time { get; set; }
    }

    public partial class Cache
    {
    }

    public partial class Request
    {
        [JsonProperty("method")]
        public string Method { get; set; }

        [JsonProperty("url")]
        public Uri Url { get; set; }

        [JsonProperty("httpVersion")]
        public string HttpVersion { get; set; }

        [JsonProperty("cookies")]
        public List<RequestCooky> Cookies { get; set; }

        [JsonProperty("headers")]
        public List<Header> Headers { get; set; }

        [JsonProperty("queryString")]
        public List<Header> QueryString { get; set; }

        [JsonProperty("headersSize")]
        public long HeadersSize { get; set; }

        [JsonProperty("bodySize")]
        public long BodySize { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class RequestCooky
    {
        [JsonProperty("name")]
        public string Name { get; set; }

        [JsonProperty("value")]
        public string Value { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class Header
    {
        [JsonProperty("name")]
        public string Name { get; set; }

        [JsonProperty("value")]
        public string Value { get; set; }
    }

    public partial class Response
    {
        [JsonProperty("status")]
        public long Status { get; set; }

        [JsonProperty("statusText")]
        public string StatusText { get; set; }

        [JsonProperty("httpVersion")]
        public string HttpVersion { get; set; }

        [JsonProperty("cookies")]
        public List<ResponseCooky> Cookies { get; set; }

        [JsonProperty("headers")]
        public List<Header> Headers { get; set; }

        [JsonProperty("content")]
        public Content Content { get; set; }

        [JsonProperty("redirectURL")]
        public string RedirectUrl { get; set; }

        [JsonProperty("headersSize")]
        public long HeadersSize { get; set; }

        [JsonProperty("bodySize")]
        public long BodySize { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class Content
    {
        [JsonProperty("size")]
        public long Size { get; set; }

        [JsonProperty("mimeType")]
        public string MimeType { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class ResponseCooky
    {
        [JsonProperty("name")]
        public string Name { get; set; }

        [JsonProperty("value")]
        public string Value { get; set; }

        [JsonProperty("path")]
        public string Path { get; set; }

        [JsonProperty("domain")]
        public string Domain { get; set; }

        [JsonProperty("expires")]
        public DateTimeOffset Expires { get; set; }

        [JsonProperty("httpOnly")]
        public bool HttpOnly { get; set; }

        [JsonProperty("secure")]
        public bool Secure { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class Timings
    {
        [JsonProperty("comment")]
        public string Comment { get; set; }

        [JsonProperty("dns")]
        public long Dns { get; set; }

        [JsonProperty("connect")]
        public long Connect { get; set; }

        [JsonProperty("send")]
        public long Send { get; set; }

        [JsonProperty("wait")]
        public long Wait { get; set; }

        [JsonProperty("receive")]
        public long Receive { get; set; }

        [JsonProperty("blocked")]
        public long Blocked { get; set; }

        [JsonProperty("ssl")]
        public long Ssl { get; set; }
    }

    public partial class Page
    {
        [JsonProperty("id")]
        public Uri Id { get; set; }

        [JsonProperty("startedDateTime")]
        public DateTimeOffset StartedDateTime { get; set; }

        [JsonProperty("title")]
        public Uri Title { get; set; }

        [JsonProperty("pageTimings")]
        public PageTimings PageTimings { get; set; }

        [JsonProperty("comment")]
        public string Comment { get; set; }
    }

    public partial class PageTimings
    {
        [JsonProperty("comment")]
        public string Comment { get; set; }
    }








}


