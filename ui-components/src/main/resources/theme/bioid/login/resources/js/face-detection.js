/**
 * Face Detection and Image Processing for BioID
 * 
 * This module provides face detection, cropping, and resizing functionality
 * optimized for BioID BWS requirements:
 * - Recommended size: 1200x1600 pixels
 * - Lossless compression (JPEG quality 0.95)
 * - Face-centered with appropriate padding
 */

class FaceDetector {
    constructor() {
        this.faceDetector = null;
        this.isReady = false;
        this.targetWidth = 1200;
        this.targetHeight = 1600;
        this.jpegQuality = 0.95;
        this.facePadding = 0.4; // 40% padding around detected face
    }

    /**
     * Initialize the face detector using browser's built-in FaceDetector API
     * Falls back to simple center crop if not available
     */
    async initialize() {
        try {
            if ('FaceDetector' in window) {
                this.faceDetector = new FaceDetector({
                    maxDetectedFaces: 1,
                    fastMode: false
                });
                this.isReady = true;
                console.log('Face detection initialized successfully');
                return true;
            } else {
                console.warn('FaceDetector API not available, using fallback');
                this.isReady = true;
                return false;
            }
        } catch (error) {
            console.warn('Face detection initialization failed:', error);
            this.isReady = true;
            return false;
        }
    }

    /**
     * Detect faces in the video frame
     * @param {HTMLVideoElement|HTMLCanvasElement|HTMLImageElement} source
     * @returns {Promise<Array>} Array of detected faces
     */
    async detectFaces(source) {
        if (!this.faceDetector) {
            console.log('Face detector not available, skipping detection');
            return [];
        }
        
        if (typeof this.faceDetector.detect !== 'function') {
            console.warn('Face detector does not have detect method');
            this.faceDetector = null;
            return [];
        }

        try {
            const faces = await this.faceDetector.detect(source);
            return faces || [];
        } catch (error) {
            console.warn('Face detection failed, will use fallback:', error.message);
            // Disable face detector on error to use fallback
            this.faceDetector = null;
            return [];
        }
    }

    /**
     * Process and crop image to face with BioID-optimized settings
     * @param {HTMLVideoElement} video - Video element to capture from
     * @param {boolean} unMirror - Whether to un-mirror the image (default: true for correct orientation)
     * @returns {Promise<string>} Base64 encoded JPEG image
     */
    async processImage(video, unMirror = true) {
        // Create temporary canvas for capture
        const captureCanvas = document.createElement('canvas');
        captureCanvas.width = video.videoWidth;
        captureCanvas.height = video.videoHeight;
        const captureCtx = captureCanvas.getContext('2d');
        
        // Draw video frame
        // Note: Video is displayed mirrored for user comfort, but we capture un-mirrored
        // so that head movements match the actual direction (right = right, left = left)
        if (unMirror) {
            // Draw normally (un-mirrored) for correct orientation
            captureCtx.drawImage(video, 0, 0, captureCanvas.width, captureCanvas.height);
        } else {
            // Draw mirrored (flip horizontally)
            captureCtx.save();
            captureCtx.scale(-1, 1);
            captureCtx.drawImage(video, -captureCanvas.width, 0, captureCanvas.width, captureCanvas.height);
            captureCtx.restore();
        }

        // Detect face
        const faces = await this.detectFaces(captureCanvas);
        
        let cropRegion;
        if (faces && faces.length > 0) {
            // Use detected face
            cropRegion = this.calculateCropRegion(faces[0].boundingBox, captureCanvas.width, captureCanvas.height);
            console.log('Face detected, cropping to face region');
        } else {
            // Fallback: center crop with portrait aspect ratio
            cropRegion = this.calculateCenterCrop(captureCanvas.width, captureCanvas.height);
            console.log('No face detected, using center crop');
        }

        // Create output canvas with target dimensions
        const outputCanvas = document.createElement('canvas');
        outputCanvas.width = this.targetWidth;
        outputCanvas.height = this.targetHeight;
        const outputCtx = outputCanvas.getContext('2d');

        // Draw cropped and resized image
        outputCtx.drawImage(
            captureCanvas,
            cropRegion.x, cropRegion.y, cropRegion.width, cropRegion.height,
            0, 0, this.targetWidth, this.targetHeight
        );

        // Convert to JPEG with high quality
        return outputCanvas.toDataURL('image/jpeg', this.jpegQuality);
    }

    /**
     * Calculate crop region based on detected face with padding
     * @param {DOMRect} faceBounds - Detected face bounding box
     * @param {number} imageWidth - Source image width
     * @param {number} imageHeight - Source image height
     * @returns {Object} Crop region {x, y, width, height}
     */
    calculateCropRegion(faceBounds, imageWidth, imageHeight) {
        // Add padding around face
        const paddingX = faceBounds.width * this.facePadding;
        const paddingY = faceBounds.height * this.facePadding;
        
        let x = faceBounds.x - paddingX;
        let y = faceBounds.y - paddingY;
        let width = faceBounds.width + (paddingX * 2);
        let height = faceBounds.height + (paddingY * 2);

        // Adjust to maintain 3:4 aspect ratio (1200:1600)
        const targetAspect = this.targetWidth / this.targetHeight; // 0.75
        const currentAspect = width / height;

        if (currentAspect > targetAspect) {
            // Too wide, increase height
            const newHeight = width / targetAspect;
            const heightDiff = newHeight - height;
            y -= heightDiff / 2;
            height = newHeight;
        } else {
            // Too tall, increase width
            const newWidth = height * targetAspect;
            const widthDiff = newWidth - width;
            x -= widthDiff / 2;
            width = newWidth;
        }

        // Ensure crop region is within image bounds
        if (x < 0) {
            width += x;
            x = 0;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        if (x + width > imageWidth) {
            width = imageWidth - x;
        }
        if (y + height > imageHeight) {
            height = imageHeight - y;
        }

        return { x, y, width, height };
    }

    /**
     * Calculate center crop with portrait aspect ratio (fallback)
     * @param {number} imageWidth - Source image width
     * @param {number} imageHeight - Source image height
     * @returns {Object} Crop region {x, y, width, height}
     */
    calculateCenterCrop(imageWidth, imageHeight) {
        const targetAspect = this.targetWidth / this.targetHeight; // 0.75
        const currentAspect = imageWidth / imageHeight;

        let width, height, x, y;

        if (currentAspect > targetAspect) {
            // Image is wider than target, crop width
            height = imageHeight;
            width = height * targetAspect;
            x = (imageWidth - width) / 2;
            y = 0;
        } else {
            // Image is taller than target, crop height
            width = imageWidth;
            height = width / targetAspect;
            x = 0;
            y = (imageHeight - height) / 2;
        }

        return { x, y, width, height };
    }

    /**
     * Check if a face is detected and well-positioned
     * @param {HTMLVideoElement} video - Video element to check
     * @returns {Promise<Object>} Detection result with quality metrics
     */
    async checkFaceQuality(video) {
        const captureCanvas = document.createElement('canvas');
        captureCanvas.width = video.videoWidth;
        captureCanvas.height = video.videoHeight;
        const captureCtx = captureCanvas.getContext('2d');
        
        // For face detection, we can use the mirrored view since we're just checking quality
        // The actual capture will be un-mirrored
        captureCtx.drawImage(video, 0, 0, captureCanvas.width, captureCanvas.height);

        const faces = await this.detectFaces(captureCanvas);
        
        if (!faces || faces.length === 0) {
            return {
                detected: false,
                quality: 'none',
                message: 'No face detected'
            };
        }

        const face = faces[0];
        const faceBounds = face.boundingBox;
        
        // Check if face is reasonably sized (not too small or too large)
        const faceArea = faceBounds.width * faceBounds.height;
        const imageArea = captureCanvas.width * captureCanvas.height;
        const faceRatio = faceArea / imageArea;

        // Check if face is centered
        const faceCenterX = faceBounds.x + faceBounds.width / 2;
        const faceCenterY = faceBounds.y + faceBounds.height / 2;
        const imageCenterX = captureCanvas.width / 2;
        const imageCenterY = captureCanvas.height / 2;
        const offsetX = Math.abs(faceCenterX - imageCenterX) / captureCanvas.width;
        const offsetY = Math.abs(faceCenterY - imageCenterY) / captureCanvas.height;

        let quality = 'good';
        let message = 'Face detected';

        if (faceRatio < 0.05) {
            quality = 'too-far';
            message = 'Move closer to the camera';
        } else if (faceRatio > 0.5) {
            quality = 'too-close';
            message = 'Move back from the camera';
        } else if (offsetX > 0.2 || offsetY > 0.2) {
            quality = 'off-center';
            message = 'Center your face in the frame';
        }

        return {
            detected: true,
            quality,
            message,
            bounds: faceBounds,
            faceRatio,
            offsetX,
            offsetY
        };
    }
}

// Export for use in enrollment/authentication pages
window.FaceDetector = FaceDetector;
