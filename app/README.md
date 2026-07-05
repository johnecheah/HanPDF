# HanPDF - Premium PDF Editor & Productivity Suite

HanPDF is a powerful, offline-first PDF editing and document scanning application crafted entirely in modern **Kotlin** and **Jetpack Compose**. Featuring a beautiful, bento-inspired Material Design 3 user interface, HanPDF delivers a desktop-grade document utility hub directly to your Android device.

---

## 🌟 Core Features

### 📂 1. Bento Dashboard & Document Hub
*   **Contextual Actions**: Perform quick operations on your files: search in real-time, star critical documents for rapid access, or filter items using categories such as *Combine*, *Scan*, *ID Card*, or *Blank Doc*.
*   **Multi-Select Engine**: Select multiple documents simultaneously to delete them in batches or merge them together instantly using the integrated PDF Merger tool.
*   **Recent Files**: Easily resume your work with a dedicated, beautifully formatted recents history tab.

### 🎨 2. Advanced PDF Editor & Drawing Canvas
*   **Layered Interaction**: Load multi-page documents and effortlessly paint, annotate, or sign directly over the pages.
*   **Fluid Drawing Layer**: Draw freehand notes using a highly responsive canvas. Fully customize stroke width, opacity, and choose from an extensive color palette (or input any precise Hex code).
*   **Undo/Redo History**: Features a robust historical stack, allowing you to easily roll back or re-apply annotations and brushstrokes.

### 📝 3. Desktop-Grade Text Annotations
*   **Interactive Placement**: Place text anywhere on the document canvas, then tap to edit.
*   **Rich Formatting**: Fine-tune your typography with toggles for **Bold**, *Italic*, __Underline__, and ~~Strikethrough~~.
*   **Advanced Alignment**: Align text to the `Left`, `Center`, or `Right` within the annotation frame.
*   **Styling Customization**: Select from popular fonts (such as *Arial*, *Times New Roman*, *Courier*, and *Georgia*), scale text dynamically, choose text & background colors, or toggle custom border outlines.

### 🖋️ 4. Professional Signature Studio
*   **Vector Signature Canvas**: Draw signature profiles with smooth freehand pen strokes and save them to your profile hub.
*   **Signature Import**: Import physical signatures from device photos. The app automatically applies high-contrast filters to remove backgrounds, converting them into clean vector overlays.
*   **Interactive Drag & Resize**: Place any saved signature onto your PDF page, then resize, drag, or rotate it with intuitive gesture controls.

### 🪪 5. ID Security Scan Lab (ID Card Scanner)
*   **Dual-Sided Capture**: Scan or import both the Front and Back sides of an ID card (such as a Driver's License, National ID, or Badge).
*   **Standardized Aspect Ratio**: Automatically crop scans using a realistic credit card aspect ratio ($86\text{mm} \times 54\text{mm}$).
*   **Single-Page layout**: Compiles both sides neatly onto a single A4 template, ready to save or share.
*   **Legibility Boost**: Updated both the live interactive preview and saved output coordinates to **increase the physical card size by +30%**, delivering exceptional legibility and crisp details.

### 📄 6. High-Fidelity Sandbox Document Scanner
*   **Real-time Animations**: Features an immersive superimposed laser-beam scanning animation and custom guiding visualizer.
*   **Automatic Edge Detection**: Guides the user to frame documents optimally.
*   **Enhancement Filters**: Enhance scan readability using filters like *Document/Enhance*, *Grayscale*, and *High-Contrast Mono*.

### 🔀 7. Acrobat Combine PDF Workbench (Merger)
*   **Dynamic Document Assembly**: Pick multiple files from your local storage or dashboard history.
*   **Drag-and-Drop Ordering**: Arrange the compilation sequence smoothly to synthesize a brand new, multi-page document.

### 🔍 8. Optical Character Recognition (OCR) Text Recognition
*   **On-Device AI Transcription**: Instantly extract text from scanned pages.
*   **Editable Transcript Panel**: Review, edit, or copy the transcription to your clipboard with a single tap.

---

## 🛠️ Architecture & Tech Stack

HanPDF follows industry-standard Android development practices:

*   **UI Framework**: Built entirely in **Jetpack Compose**, utilizing standard Material Design 3 guidelines for responsive density, spacing grids, and edge-to-edge layouts (`enableEdgeToEdge()`).
*   **Architecture Pattern**: **MVVM (Model-View-ViewModel)** with clean separation of concerns:
    *   `MainActivityPresenter.kt`: Handles Android lifecycle activities, file picking, permissions, and ML-Kit document scan routing.
    *   `MainViewModel.kt`: Implements the central business logic, drawing states, OCR operations, document creation, and PDF rendering math.
    *   `PdfAppScreens.kt`: The composable layer housing the Bento Dashboard, the Sandbox Scanner, the Signature Studio, the Editor, and dialogs.
*   **Reactive Flow**: Managed using Kotlin `StateFlow` and structured coroutines to ensure synchronous file-saving and asynchronous drawing operations remain non-blocking.
*   **Resource Identification**: Key interactive views contain unique `testTag` semantics for robust automated UI testing.

---

## 🚀 Getting Started

### Prerequisites
*   Android SDK 34 (Upside Down Cake) or higher.
*   Android Studio Jellyfish / Koala or newer.
*   Gradle 8.4+ with Kotlin DSL support.

### Building and Running the App
1. Clone or import the project directory.
2. Ensure you have internet permission configured in your `AndroidManifest.xml` (already included).
3. Build the project using Gradle:
   ```bash
   gradle assembleDebug
   ```
4. Run the debug APK on your target device or local JVM using standard emulator tools.
