$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
New-Item -ItemType Directory -Force -Path 'samples' | Out-Null
$samplePath = Join-Path (Get-Location) 'samples\a-quiet-reading-moment.epub'
if (Test-Path -LiteralPath $samplePath) { Remove-Item -LiteralPath $samplePath }
$sampleArchive = [System.IO.Compression.ZipFile]::Open($samplePath, [System.IO.Compression.ZipArchiveMode]::Create)
$sampleEntries = [ordered]@{
 'mimetype' = 'application/epub+zip'
 'META-INF/container.xml' = '<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="EPUB/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>'
 'EPUB/content.opf' = '<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">urn:uuid:reading-habit-coach-sample</dc:identifier><dc:title>A Quiet Reading Moment</dc:title><dc:creator>Reading Habit Coach</dc:creator><dc:language>en-GB</dc:language><meta property="dcterms:modified">2026-10-20T00:00:00Z</meta></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="one" href="one.xhtml" media-type="application/xhtml+xml"/><item id="two" href="two.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="one"/><itemref idref="two"/></spine></package>'
 'EPUB/nav.xhtml' = '<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><h1>Contents</h1><ol><li><a href="one.xhtml">Making room</a></li><li><a href="two.xhtml">A thought to keep</a></li></ol></nav></body></html>'
 'EPUB/one.xhtml' = '<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>Making room</title></head><body><h1>Making room</h1><p>Some days, a whole chapter fits into the afternoon. Other days, there is room for only a page. Both are a beginning.</p><p>Set the cup down. Find the ribbon where you left it. There is no race waiting on the other side of this paragraph.</p><p>A reading habit grows in the spaces that already belong to you: a few minutes before breakfast, the quiet end of a journey, a chair by the window.</p><p>Read slowly enough to notice what interests you. The next page will still be there.</p><p>This original sample was written for Reading Habit Coach. It is provided for testing document import, reflow, bookmarks and selected-text reading. It is not a complete published book.</p></body></html>'
 'EPUB/two.xhtml' = '<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>A thought to keep</title></head><body><h1>A thought to keep</h1><p>A sentence sometimes follows us out of a book and into the rest of the day. Write it down if you would like to meet it again.</p><p>Ask a question. Leave a note. You do not have to capture everything. A reading journal is a place for the things that matter to you.</p><p>When you close the book, the minutes remain yours. There is always another quiet moment to begin.</p></body></html>'
}
try { foreach ($sampleEntry in $sampleEntries.GetEnumerator()) {
 $sampleCompression = if ($sampleEntry.Key -eq 'mimetype') { [System.IO.Compression.CompressionLevel]::NoCompression } else { [System.IO.Compression.CompressionLevel]::Optimal }
 $sampleZipEntry = $sampleArchive.CreateEntry($sampleEntry.Key, $sampleCompression)
 $sampleWriter = [System.IO.StreamWriter]::new($sampleZipEntry.Open(), [System.Text.UTF8Encoding]::new($false))
 $sampleWriter.Write($sampleEntry.Value); $sampleWriter.Dispose()
} } finally { $sampleArchive.Dispose() }
Write-Output $samplePath
