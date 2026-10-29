/* Orhan Compose theme — code page renderer.
   Reads window.ORHAN_FILES (written by build_code_page.py) and renders
   one card per file with line numbers, Kotlin highlighting and a copy button. */
(function () {
  "use strict";

  var FILES = window.ORHAN_FILES || [];

  /* ---------------- Kotlin highlighter ---------------- */

  var KW = new Set(
    (
      "package import val var fun class object data enum interface private public internal " +
      "override return if else when for while in is as null true false this super by get set " +
      "companion const lateinit open abstract sealed inline suspend tailrec operator infix " +
      "init constructor typealias out where try catch finally throw do break continue"
    ).split(" ")
  );

  function esc(s) {
    return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  }

  function highlight(src) {
    var out = "";
    var i = 0;
    var n = src.length;
    var NL = String.fromCharCode(10);

    while (i < n) {
      var ch = src[i];

      // line comment
      if (ch === "/" && src[i + 1] === "/") {
        var j = src.indexOf(NL, i);
        if (j === -1) j = n;
        out += '<span class="cm">' + esc(src.slice(i, j)) + "</span>";
        i = j;
        continue;
      }

      // block comment
      if (ch === "/" && src[i + 1] === "*") {
        var k = src.indexOf("*/", i + 2);
        k = k === -1 ? n : k + 2;
        out += '<span class="cm">' + esc(src.slice(i, k)) + "</span>";
        i = k;
        continue;
      }

      // string literal
      if (ch === '"') {
        var s = i + 1;
        while (s < n) {
          if (src[s] === "\\") { s += 2; continue; }
          if (src[s] === '"') { s++; break; }
          if (src[s] === NL) break;
          s++;
        }
        out += '<span class="s">' + esc(src.slice(i, s)) + "</span>";
        i = s;
        continue;
      }

      // char literal
      if (ch === "'") {
        var c = i + 1;
        while (c < n) {
          if (src[c] === "\\") { c += 2; continue; }
          if (src[c] === "'") { c++; break; }
          c++;
        }
        out += '<span class="s">' + esc(src.slice(i, c)) + "</span>";
        i = c;
        continue;
      }

      // annotation
      if (ch === "@" && /[A-Za-z]/.test(src[i + 1] || "")) {
        var a = i + 1;
        while (a < n && /[A-Za-z0-9_]/.test(src[a])) a++;
        out += '<span class="a">' + esc(src.slice(i, a)) + "</span>";
        i = a;
        continue;
      }

      // number
      if (/[0-9]/.test(ch) && !/[A-Za-z0-9_]/.test(src[i - 1] || "")) {
        var d = i;
        while (d < n && /[0-9a-fA-FxX._]/.test(src[d])) d++;
        out += '<span class="n">' + esc(src.slice(i, d)) + "</span>";
        i = d;
        continue;
      }

      // identifier
      if (/[A-Za-z_]/.test(ch)) {
        var w = i;
        while (w < n && /[A-Za-z0-9_]/.test(src[w])) w++;
        var word = src.slice(i, w);
        var cls = null;
        if (KW.has(word)) cls = "k";
        else if (/^[A-Z]/.test(word)) cls = "t";
        else if (src[w] === "(") cls = "f";
        out += cls ? '<span class="' + cls + '">' + esc(word) + "</span>" : esc(word);
        i = w;
        continue;
      }

      out += esc(ch);
      i++;
    }
    return out;
  }

  /* ---------------- render ---------------- */

  var nav = document.getElementById("nav");
  var main = document.getElementById("main");
  var NL = String.fromCharCode(10);

  FILES.forEach(function (f, idx) {
    var a = document.createElement("a");
    a.href = "#" + f.id;
    a.dataset.id = f.id;
    a.innerHTML =
      '<span class="n">' + String(idx + 1).padStart(2, "0") + "</span>" +
      "<span>" + f.name + "</span>" +
      '<span class="ln">' + f.lines + "</span>";
    nav.appendChild(a);

    var card = document.createElement("section");
    card.className = "file";
    card.id = f.id;

    var head = document.createElement("div");
    head.className = "fhead";
    head.innerHTML =
      '<span class="fname">' + f.name + "</span>" +
      '<span class="fdesc">' + f.desc + "</span>" +
      '<span class="flines">' + f.lines + " lines</span>" +
      '<button class="copy" type="button" data-id="' + f.id + '">' +
      '<svg viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2"/>' +
      '<path d="M5 15V5a2 2 0 0 1 2-2h10"/></svg><span>Copy</span></button>';

    var wrap = document.createElement("div");
    wrap.className = "codewrap";
    var pre = document.createElement("pre");
    var code = document.createElement("code");

    var body = f.code.replace(/\s+$/, "");
    var hl = highlight(body).split(NL);

    code.innerHTML = hl
      .map(function (line, i) {
        return (
          '<span class="ln"><span class="g">' + (i + 1) +
          '</span><span class="c">' + line + "</span></span>"
        );
      })
      .join("");

    pre.appendChild(code);
    wrap.appendChild(pre);
    card.appendChild(head);
    card.appendChild(wrap);
    main.appendChild(card);
  });

  /* ---------------- copy ---------------- */

  main.addEventListener("click", function (e) {
    var btn = e.target.closest(".copy");
    if (!btn) return;
    var f = FILES.filter(function (x) { return x.id === btn.dataset.id; })[0];
    if (!f) return;

    var done = function () {
      var lbl = btn.querySelector("span");
      var old = lbl.textContent;
      btn.classList.add("done");
      lbl.textContent = "Copied";
      setTimeout(function () {
        btn.classList.remove("done");
        lbl.textContent = old;
      }, 1600);
    };

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(f.code).then(done, function () { fallback(f.code, done); });
    } else {
      fallback(f.code, done);
    }
  });

  function fallback(text, done) {
    var ta = document.createElement("textarea");
    ta.value = text;
    ta.setAttribute("readonly", "");
    ta.style.position = "fixed";
    ta.style.opacity = "0";
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand("copy"); } catch (err) { /* ignore */ }
    ta.remove();
    done();
  }

  /* ---------------- scroll spy ---------------- */

  var links = Array.prototype.slice.call(nav.querySelectorAll("a"));
  if ("IntersectionObserver" in window) {
    var obs = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (en) {
          if (en.isIntersecting) {
            links.forEach(function (l) {
              l.classList.toggle("on", l.dataset.id === en.target.id);
            });
          }
        });
      },
      { rootMargin: "-10% 0px -70% 0px" }
    );
    Array.prototype.forEach.call(document.querySelectorAll(".file"), function (el) {
      obs.observe(el);
    });
  }

  /* ---------------- theme ---------------- */

  var root = document.documentElement;
  var btn = document.getElementById("themeBtn");
  var lbl = document.getElementById("themeLbl");

  function apply(mode) {
    root.setAttribute("data-theme", mode);
    lbl.textContent = mode === "dark" ? "Light" : "Dark";
    try { localStorage.setItem("orhan-code-theme", mode); } catch (err) { /* ignore */ }
  }

  var saved = "dark";
  try { saved = localStorage.getItem("orhan-code-theme") || "dark"; } catch (err) { /* ignore */ }
  apply(saved);

  btn.addEventListener("click", function () {
    apply(root.getAttribute("data-theme") === "dark" ? "light" : "dark");
  });
})();
