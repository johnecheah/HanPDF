import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';

// Use standard CDN for pdf.worker to avoid local configuration boilerplate
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

/**
 * ReactPdfViewer Component - Bento Grid Inspired Design
 * Fully featured React component to render and display PDF documents uploaded by the user
 * with robust navigation, pagination, scaling, rotation, and file-upload states.
 */
export default function ReactPdfViewer() {
  const [pdfFile, setPdfFile] = useState(null);
  const [numPages, setNumPages] = useState(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [scale, setScale] = useState(1.0);
  const [rotation, setRotation] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Handle PDF file upload via standard input
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      loadFile(file);
    }
  };

  // Safe loader that validates the uploaded MIME type
  const loadFile = (file) => {
    if (file.type !== "application/pdf" && !file.name.endsWith('.pdf')) {
      setErrorMsg("Invalid file format. Please select or drop a valid PDF file.");
      setPdfFile(null);
      return;
    }
    setErrorMsg('');
    setPdfFile(file);
    setPageNumber(1);
    setScale(1.0);
    setRotation(0);
  };

  // Drag and drop event handlers
  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) {
      loadFile(file);
    }
  };

  // Document metadata loading callbacks
  const onDocumentLoadSuccess = ({ numPages }) => {
    setNumPages(numPages);
    setPageNumber(1);
    setErrorMsg('');
  };

  const onDocumentLoadError = (err) => {
    setErrorMsg(`Failed to parse your PDF document: ${err.message}`);
    setPdfFile(null);
  };

  // Page index navigations
  const changePage = (offset) => {
    setPageNumber((prevPageNumber) => {
      const next = prevPageNumber + offset;
      return Math.min(Math.max(next, 1), numPages || 1);
    });
  };

  // Quick controls
  const zoomIn = () => setScale((prev) => Math.min(prev + 0.15, 2.5));
  const zoomOut = () => setScale((prev) => Math.max(prev - 0.15, 0.5));
  const rotateRight = () => setRotation((prev) => (prev + 90) % 360);
  const resetViewer = () => {
    setPdfFile(null);
    setNumPages(null);
    setPageNumber(1);
    setScale(1.0);
    setRotation(0);
    setErrorMsg('');
  };

  return (
    <div className="min-h-screen bg-[#F7F9FF] text-slate-900 font-sans p-6 flex flex-col items-center">
      
      {/* Bento Grid layout wrapping header and component */}
      <div className="w-full max-w-4xl flex flex-col gap-6">
        
        {/* Header Block */}
        <header className="flex items-center justify-between bg-white rounded-3xl p-5 border border-slate-100 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-[#005FB8] rounded-xl flex items-center justify-center text-white font-bold">
              PDF
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-[#001C3B]">OmniPDF Web Renderer</h1>
              <p className="text-xs text-slate-500">React + pdfjs pagination deck</p>
            </div>
          </div>
          {pdfFile && (
            <button
              onClick={resetViewer}
              className="px-4 py-2 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 active:scale-95 transition rounded-full border border-red-100"
            >
              Clear File
            </button>
          )}
        </header>

        {/* Dynamic State Card */}
        {!pdfFile ? (
          /* File Upload Drop Zone Component */
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`flex flex-col items-center justify-center border-2 border-dashed rounded-[32px] p-12 transition min-h-[350px] bg-white ${
              isDragging
                ? 'border-[#005FB8] bg-[#D3E4FF]/40 scale-98'
                : 'border-slate-200 hover:border-[#005FB8]/40'
            }`}
          >
            <div className="w-16 h-16 bg-[#D3E4FF] rounded-2xl flex items-center justify-center text-[#005FB8] mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
            </div>
            
            <h3 className="text-lg font-bold text-[#001C3B] mb-1">Upload your PDF</h3>
            <p className="text-sm text-slate-500 text-center max-w-sm mb-6">
              Drag and drop your document here, or choose a file from your hard drive local storage.
            </p>

            <label className="cursor-pointer bg-[#005FB8] text-white hover:bg-[#004f99] active:scale-95 transition px-6 py-3 rounded-full text-sm font-semibold shadow-md inline-block">
              Select Document File
              <input
                type="file"
                accept="application/pdf"
                className="hidden"
                onChange={handleFileChange}
              />
            </label>

            {errorMsg && (
              <div className="mt-6 bg-red-50 border border-red-100 text-red-700 text-xs font-semibold px-4 py-3 rounded-2xl max-w-md text-center">
                {errorMsg}
              </div>
            )}
          </div>
        ) : (
          /* Realtime Interactive Document Viewer Card */
          <div className="flex flex-col gap-4">
            
            {/* Control Toolbar Deck (Mini Bento Card) */}
            <div className="bg-white rounded-2xl p-3 border border-slate-100 shadow-sm flex flex-wrap items-center justify-between gap-3">
              
              {/* Pagination Controls */}
              <div className="flex items-center gap-2">
                <button
                  disabled={pageNumber <= 1}
                  onClick={() => changePage(-1)}
                  className="w-10 h-10 rounded-xl bg-[#EDF1F9] hover:bg-slate-200 text-[#001C3B] active:scale-90 transition flex items-center justify-center disabled:opacity-40 disabled:pointer-events-none"
                  title="Previous Page"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                </button>

                <div className="bg-[#EDF1F9] px-4 py-2 rounded-xl text-xs font-bold text-[#001C3B] min-w-[100px] text-center">
                  Page {pageNumber} of {numPages || '...'}
                </div>

                <button
                  disabled={numPages === null || pageNumber >= numPages}
                  onClick={() => changePage(1)}
                  className="w-10 h-10 rounded-xl bg-[#EDF1F9] hover:bg-slate-200 text-[#001C3B] active:scale-90 transition flex items-center justify-center disabled:opacity-40 disabled:pointer-events-none"
                  title="Next Page"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414s-4 4-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                  </svg>
                </button>
              </div>

              {/* Document metadata display */}
              <div className="hidden md:block truncate max-w-xs text-xs font-semibold text-slate-500">
                📄 {pdfFile.name}
              </div>

              {/* View/Layout Settings Controls */}
              <div className="flex items-center gap-2">
                
                {/* Scale Zoom controls */}
                <div className="flex items-center gap-1 bg-[#EDF1F9] p-1 rounded-xl">
                  <button
                    onClick={zoomOut}
                    className="w-8 h-8 rounded-lg hover:bg-white text-[#001C3B] active:scale-90 transition flex items-center justify-center font-bold"
                    title="Zoom Out"
                  >
                    -
                  </button>
                  <span className="text-[10px] font-extrabold px-2 text-[#001C3B] min-w-[50px] text-center">
                    {Math.round(scale * 100)}%
                  </span>
                  <button
                    onClick={zoomIn}
                    className="w-8 h-8 rounded-lg hover:bg-white text-[#001C3B] active:scale-90 transition flex items-center justify-center font-bold"
                    title="Zoom In"
                  >
                    +
                  </button>
                </div>

                {/* Rotate control */}
                <button
                  onClick={rotateRight}
                  className="w-10 h-10 rounded-xl bg-[#EDF1F9] hover:bg-slate-200 text-[#001C3B] active:scale-90 transition flex items-center justify-center"
                  title="Rotate Right"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 8H17" />
                  </svg>
                </button>
              </div>

            </div>

            {/* Main Stage Display Canvas */}
            <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center justify-center overflow-auto min-h-[450px]">
              <Document
                file={pdfFile}
                onLoadSuccess={onDocumentLoadSuccess}
                onLoadError={onDocumentLoadError}
                loading={
                  <div className="flex flex-col items-center justify-center gap-3">
                    <div className="w-8 h-8 border-4 border-[#005FB8] border-t-transparent animate-spin rounded-full"></div>
                    <p className="text-xs font-semibold text-slate-500">Preparing PDF document canvas...</p>
                  </div>
                }
              >
                <Page
                  pageNumber={pageNumber}
                  scale={scale}
                  rotate={rotation}
                  renderTextLayer={true}
                  renderAnnotationLayer={false}
                  className="shadow-md rounded-lg overflow-hidden max-w-full"
                />
              </Document>
            </div>

          </div>
        )}

      </div>
    </div>
  );
}
