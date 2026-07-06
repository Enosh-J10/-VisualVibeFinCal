# Smart Scan: Receipt OCR

Smart Scan allows users to automate expense entry by scanning physical receipts using the device camera or uploading images/PDFs.

## Technology Stack
- **Camera**: CameraX (`PreviewView`).
- **OCR Engine**: Google ML Kit Text Recognition (Latin).
- **PDF Support**: `PdfRenderer` for converting PDF pages to bitmaps.

## Performance & Memory (OOM Prevention)
- **Downsampling**: To prevent `OutOfMemoryError` on high-resolution cameras, images are processed in two passes:
    1. `inJustDecodeBounds = true` to measure dimensions.
    2. Decoded with a calculated `inSampleSize` to target a maximum of **2048px**.
- **Preprocessing**: Bitmaps are converted to grayscale and have their contrast adjusted before OCR to improve accuracy on faint thermal paper receipts.

## Extraction Logic (`processUri` function)
- **Merchant**: Searches the top 5 lines for likely company names, filtering out keywords like "RECEIPT" or "INVOICE".
- **Total**: Uses a priority-based keyword search ("TOTAL", "NET", "AMOUNT DUE") and regex to identify currency patterns.
- **Date**: Matches standard date formats (DD/MM/YYYY, etc.) using regex.
- **VAT**: Identifies tax components separately for detailed logging.

## Permissions
- **Camera**: Required for live scanning.
- **Photo Picker**: Used for file/gallery uploads (No broad storage permissions required).

## Pitfalls
- Long receipts may require the user to hold the camera further back, which can reduce OCR accuracy.
- Thermal paper receipts with very low contrast are the primary failure point.

## Testing Checklist
- [ ] Scan a physical receipt.
- [ ] Upload a clear JPG from the gallery.
- [ ] Upload a single-page PDF invoice.
- [ ] Ensure large images (12MP+) do not crash the app.
