# HanPDF – All-in-One Mobile Document & PDF Studio

**HanPDF** is an Adobe Acrobat-style document suite for Android, built with **Kotlin** and **Jetpack Compose**. Featuring a Material 3 layout, HanPDF provides tools to scan, edit, sign, merge, and extract text from files completely on-device.

---

## 🚀 Key Features

### 📱 Modern Grid Dashboard
- **Instant Hub**: A clean, colorful Material 3 dashboard that provides one-tap access to all powerful PDF utilities.
- **Visual Indicators**: Fluid layouts with responsive card transitions, high-contrast Material icons, and clear subtitles for straightforward navigation.

### 📄 High-Fidelity Document Scanner (Sandbox Mode)
- **Interactive Scanning**: Features a live simulated camera canvas with a superimposed laser beam scanning animation and edge-guide target cues.
- **Perspective Cropping**: Integrated real-time perspective crop preview to select boundaries before final processing.
- **Image Filters**: Enhances scanning clarity with native processing filters including **Original**, **Greyscale**, **Magic Color**, and high-contrast **Black & White**.
- **Multi-Page Support**: Easily capture and append pages, adjust page orders, and export directly as formatted PDF documents.

### 💳 Standardized ID Card Scanner (CR80 Special Edition)
- **Structured Dual-Side Guide**: Prompts and guides users through capture states for the Front and Back sides of standard identification or driver’s license cards.
- **Automatic Crop & Fit**: Automatically crops both captured images to the physical 8x5 ratio (standard CR80 smart card aspect ratio).
- **A4 Layout Alignment**: Renders the captured Front and Back card sides centered and stacked vertically on a high-fidelity virtual A4 canvas.
- **✨ High-Fidelity Legibility Booster**: Renders cards **30% larger** on the output A4 page, allowing details, barcodes, and text to be printed or read with maximum clarity.

### ✍️ Digital Signature Studio & Placer
- **Signature Board**: A dedicated high-resolution signature canvas with customizable pen ink, adjustable brush thickness, smooth anti-aliased strokes, and transparent PNG exporting.
- **Drag & Stamp placement**: Dynamically stamp your saved signature onto any PDF page, with full support for custom scaling, translation, and rotational adjustments.

### 📝 Rich Text Annotations & Editor
- **Custom Fonts**: Apply premium typography selections including **Arial**, **Courier**, **Times**, **Georgia**, and system default Monospace or Sans-Serif fonts.
- **Style & Formatting**: Emphasize text annotations with standalone toggles for **Bold**, *Italic*, ~~Strikethrough~~, and <u>Underline</u>.
- **Paragraph Alignment**: Effortlessly format text with **Left**, **Center**, and **Right** alignment configurations.
- **Interactive Color Palette**: Style your annotations using an exquisite color carousel featuring clean visual previews, contrast borders, and dynamic backgrounds.

### 🔍 Optical Character Recognition (OCR) Scanner
- **Local Text Extraction**: Scans documents on-device, extracting text fields within seconds.
- **Text Management**: Allows users to quickly view, edit, copy, or share extracted text outputs to clipboard or messaging apps.

### 🔀 Multi-Document PDF Merger
- **Batch Selection**: Select multiple PDFs or image files from your local storage.
- **Page Arrangement**: Seamlessly view, sort, or drag-and-drop file order before merging.
- **Output Customization**: Define custom titles, compress sizes, and merge seamlessly into a single consolidated PDF document.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Built entirely using **Jetpack Compose** for modular, declarative, and high-performance layout construction.
- **Design System**: Strictly adheres to **Material Design 3 (M3)** with custom theme schemes, container paddings, and dynamic component styling.
- **Programming Language**: **Kotlin** featuring asynchronous **Coroutines** and active **StateFlow** state management.
- **State Pattern**: **MVVM (Model-View-ViewModel)** for clear separation of concerns and deterministic rendering lifecycles.
- **Data Persistence**: Uses a local SQLite abstraction for configuration storage.

---

## 🎨 Visual Identity

HanPDF integrates beautiful design patterns:
- **Clean Contrast**: Generous negative spacing, high-contrast active highlights, and smooth, rounded surfaces.
- **Legible Previews**: Realistic rendering modules showcasing final outputs in real-time, matching exact physical dimensions.
