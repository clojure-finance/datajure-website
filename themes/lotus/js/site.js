// Datajure website: small progressive enhancements. The pages work without it.
(function () {
  "use strict";

  // Light/dark toggle. With no saved choice the system setting applies.
  var root = document.documentElement;
  var toggle = document.getElementById("theme-toggle");
  function currentTheme() {
    if (root.dataset.theme) return root.dataset.theme;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  if (toggle) {
    toggle.addEventListener("click", function () {
      var next = currentTheme() === "dark" ? "light" : "dark";
      root.dataset.theme = next;
      try { localStorage.setItem("theme", next); } catch (e) {}
    });
  }

  // Syntax highlighting. Output blocks (```text) stay plain.
  if (window.hljs) {
    document.querySelectorAll("pre code").forEach(function (el) {
      if (el.classList.contains("text")) return;
      if (el.classList.contains("clojure")) el.classList.add("language-clojure");
      window.hljs.highlightElement(el);
    });
  }

  // Copy buttons on code blocks (not on outputs).
  document.querySelectorAll(".prose pre > code:not(.text)").forEach(function (code) {
    var btn = document.createElement("button");
    btn.type = "button";
    btn.className = "copy-btn";
    btn.textContent = "Copy";
    btn.setAttribute("aria-label", "Copy code to clipboard");
    btn.addEventListener("click", function () {
      if (!navigator.clipboard) return;
      navigator.clipboard.writeText(code.innerText).then(function () {
        btn.textContent = "Copied";
        btn.classList.add("copied");
        setTimeout(function () { btn.textContent = "Copy"; btn.classList.remove("copied"); }, 1500);
      });
    });
    code.parentNode.appendChild(btn);
  });

  // Scrollable wrapper so wide tables never widen the page.
  document.querySelectorAll(".prose table").forEach(function (table) {
    if (table.closest(".table-wrap")) return;
    var wrap = document.createElement("div");
    wrap.className = "table-wrap";
    table.parentNode.insertBefore(wrap, table);
    wrap.appendChild(table);
  });

  // Callouts: "> **Warning:** ..." gets the warning style.
  document.querySelectorAll(".prose blockquote").forEach(function (q) {
    var first = q.querySelector("p > strong:first-child");
    if (first && /^(warning|caution)/i.test(first.textContent)) q.classList.add("callout-warning");
  });

  // Link icons on section headings.
  document.querySelectorAll(".prose h2[id], .prose h3[id]").forEach(function (h) {
    var a = document.createElement("a");
    a.className = "heading-anchor";
    a.href = "#" + h.id;
    a.setAttribute("aria-label", "Link to this section");
    a.textContent = "#";
    h.insertBefore(a, h.firstChild);
  });

  // Table of contents: open beside the text on wide screens, collapsed above it
  // on narrow ones; highlight the section being read.
  var tocBox = document.querySelector(".toc-box");
  if (tocBox) {
    var wide = window.matchMedia("(min-width: 1081px)");
    var syncOpen = function () { tocBox.open = wide.matches; };
    syncOpen();
    if (wide.addEventListener) wide.addEventListener("change", syncOpen);
    tocBox.querySelector("summary").addEventListener("click", function (e) {
      if (wide.matches) e.preventDefault();
    });

    var links = Array.prototype.slice.call(tocBox.querySelectorAll("a[href^='#']"));
    var byId = {};
    links.forEach(function (a) { byId[decodeURIComponent(a.getAttribute("href").slice(1))] = a; });
    var headings = links
      .map(function (a) { return document.getElementById(decodeURIComponent(a.getAttribute("href").slice(1))); })
      .filter(Boolean);
    var setActive = function () {
      var y = window.scrollY + 120, current = null;
      headings.forEach(function (h) { if (h.offsetTop <= y) current = h; });
      links.forEach(function (a) { a.classList.remove("active"); });
      if (current && byId[current.id]) byId[current.id].classList.add("active");
    };
    var ticking = false;
    window.addEventListener("scroll", function () {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(function () { setActive(); ticking = false; });
    }, { passive: true });
    setActive();
  }
})();
