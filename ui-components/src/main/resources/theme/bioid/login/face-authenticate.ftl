<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <h1>Face Authentication</h1>
        <p class="instruction-text">Position your face within the guide and follow the instructions</p>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <div id="verification-ui">
                    <div class="camera-section">
                        <div class="camera-container">
                            <video id="video" autoplay muted playsinline></video>
                            <canvas id="canvas" style="display:none;"></canvas>
                            
                            <!-- Face mask overlay -->
                            <div id="face-mask" class="face-mask">
                                <div class="face-outline">
                                    <div class="corner top-left"></div>
                                    <div class="corner top-right"></div>
                                    <div class="corner bottom-left"></div>
                                    <div class="corner bottom-right"></div>
                                </div>
                            </div>
                            
                            <!-- Status overlay -->
                            <div id="status-overlay" class="status-overlay">
                                <div id="spinner" class="spinner" style="display:none;"></div>
                                <div id="challenge-indicator" class="challenge-indicator" style="display:none;">
                                    <div id="challenge-arrow" class="challenge-arrow"></div>
                                </div>
                                <div id="challenge-text" class="challenge-text" style="display:none;">
                                    Turn your head UP
                                </div>
                                <p id="status-text">Starting camera...</p>
                            </div>
                        </div>
                    </div>
                    
                    <div class="controls-section">
                        <form id="face-auth-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="imageData" id="image-data-input" />
                            <button id="verify-btn" type="button" class="btn btn-primary btn-lg" disabled>
                                <span id="btn-text">Verify My Identity</span>
                                <div id="btn-spinner" class="btn-spinner" style="display:none;"></div>
                            </button>
                        </form>
                        
                        <div id="progress-steps" class="progress-steps" style="display:none;">
                            <div class="step" id="step-1">
                                <div class="step-circle">1</div>
                                <span class="step-label">Position Face</span>
                            </div>
                            <div class="step" id="step-2">
                                <div class="step-circle">2</div>
                                <span class="step-label">First Capture</span>
                            </div>
                            <div class="step challenge-step" id="step-3">
                                <div class="step-circle">3</div>
                                <span class="step-label">Follow Direction</span>
                            </div>
                            <div class="step challenge-step" id="step-4">
                                <div class="step-circle">4</div>
                                <span class="step-label">Second Capture</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <script src="${url.resourcesPath}/js/face-detection.js"></script>
        <script>
        (function() {
            'use strict';
            
            // Configuration from server
            var config = {
                active: ${(livenessActiveEnabled!true)?c},
                challenge: ${(livenessChallengeResponseEnabled!false)?c},
                threshold: ${livenessConfidenceThreshold!'0.5'},
                timeout: ${livenessChallengeTimeoutSeconds!'30'}
            };
            
            // Challenge directions
            var challengeDirections = ['UP', 'DOWN', 'LEFT', 'RIGHT'];
            var currentChallenge = null;
            var faceDetector = null;
            
            console.log('Face Auth Config - Active:', config.active, 'Challenge:', config.challenge, 'Threshold:', config.threshold);
            
            // Wait for DOM
            document.addEventListener('DOMContentLoaded', function() {
                console.log('Face authentication script loaded');
                
                // Initialize face detector
                if (window.FaceDetector) {
                    faceDetector = new window.FaceDetector();
                    faceDetector.initialize().then(function() {
                        console.log('Face detector initialized');
                    });
                }
                
                function findElements() {
                    var video = document.getElementById('video');
                    var canvas = document.getElementById('canvas');
                    var btn = document.getElementById('verify-btn');
                    var btnText = document.getElementById('btn-text');
                    var btnSpinner = document.getElementById('btn-spinner');
                    var input = document.getElementById('image-data-input');
                    var form = document.getElementById('face-auth-form');
                    var overlay = document.getElementById('status-overlay');
                    var statusText = document.getElementById('status-text');
                    var spinner = document.getElementById('spinner');
                    var faceMask = document.getElementById('face-mask');
                    var challengeIndicator = document.getElementById('challenge-indicator');
                    var challengeArrow = document.getElementById('challenge-arrow');
                    var progressSteps = document.getElementById('progress-steps');
                    
                    if (!video || !canvas || !btn || !input || !form) {
                        return null;
                    }
                    
                    return { 
                        video, canvas, btn, btnText, btnSpinner, input, form, overlay, statusText, 
                        spinner, faceMask, challengeIndicator, challengeArrow, progressSteps 
                    };
                }
                
                var elements = findElements();
                if (!elements) {
                    console.log('Elements not found immediately, retrying...');
                    setTimeout(function() {
                        elements = findElements();
                        if (elements) {
                            initializeFaceAuth(elements);
                        } else {
                            console.error('Elements still missing after retry');
                        }
                    }, 1000);
                } else {
                    initializeFaceAuth(elements);
                }
                
                function initializeFaceAuth(elements) {
                    var video = elements.video;
                    var canvas = elements.canvas;
                    var btn = elements.btn;
                    var btnText = elements.btnText;
                    var btnSpinner = elements.btnSpinner;
                    var input = elements.input;
                    var form = elements.form;
                    var overlay = elements.overlay;
                    var statusText = elements.statusText;
                    var spinner = elements.spinner;
                    var faceMask = elements.faceMask;
                    var challengeIndicator = elements.challengeIndicator;
                    var challengeArrow = elements.challengeArrow;
                    var progressSteps = elements.progressSteps;
                    
                    var stream = null;
                    var capturing = false;
                    var currentStep = 1;

                    // Tunable detection settings to smooth face detection and motion response
                    var detectionSettings = {
                        faceStableFrames: 3,
                        faceLostFrames: 2,
                        faceDetectionInterval: 250,
                        motionCheckInterval: 120,
                        motionDiffThreshold: 6,
                        motionChangeThreshold: 3.5,
                        motionConsecutiveFrames: 2,
                        motionMinFramesBeforeDetect: 3,
                        secondCaptureDelayMs: 350
                    };
                    
                    // Update status
                    function updateStatus(msg, showSpinner, processing) {
                        if (statusText) statusText.textContent = msg;
                        if (spinner) spinner.style.display = showSpinner ? 'block' : 'none';
                        if (overlay) {
                            // Hide overlay completely when face is detected and ready
                            if (msg.includes('Ready to verify') || msg.includes('Face detected')) {
                                overlay.style.opacity = '0';
                                setTimeout(function() {
                                    if (overlay.style.opacity === '0') {
                                        overlay.style.display = 'none';
                                    }
                                }, 300);
                            } else {
                                overlay.style.display = 'flex';
                                overlay.style.opacity = '1';
                            }
                            
                            if (processing) {
                                overlay.classList.add('processing');
                            } else {
                                overlay.classList.remove('processing');
                            }
                        }
                    }
                    
                    // Update progress steps
                    function updateStep(step) {
                        currentStep = step;
                        if (!progressSteps) return;
                        
                        for (var i = 1; i <= 4; i++) {
                            var stepEl = document.getElementById('step-' + i);
                            if (stepEl) {
                                stepEl.classList.remove('active', 'completed');
                                if (i < step) {
                                    stepEl.classList.add('completed');
                                } else if (i === step) {
                                    stepEl.classList.add('active');
                                }
                            }
                        }
                    }
                    
                    // Show challenge direction
                    function showChallenge(direction) {
                        if (!challengeIndicator || !challengeArrow) return;
                        
                        var challengeText = document.getElementById('challenge-text');
                        
                        // Hide status overlay to make challenge more prominent
                        if (overlay) overlay.style.background = 'rgba(0, 0, 0, 0.2)';
                        
                        challengeIndicator.style.display = 'block';
                        challengeArrow.className = 'challenge-arrow ' + direction.toLowerCase();
                        
                        var directionText = {
                            'UP': 'Turn your head UP',
                            'DOWN': 'Turn your head DOWN', 
                            'LEFT': 'Turn your head LEFT',
                            'RIGHT': 'Turn your head RIGHT'
                        };
                        
                        if (challengeText) {
                            challengeText.textContent = directionText[direction] || 'Follow the arrow';
                            challengeText.style.display = 'block';
                        }
                        
                        updateStatus('Follow the direction shown above', false);
                    }
                    
                    // Hide challenge
                    function hideChallenge() {
                        if (challengeIndicator) challengeIndicator.style.display = 'none';
                        var challengeText = document.getElementById('challenge-text');
                        if (challengeText) challengeText.style.display = 'none';
                        if (overlay) overlay.style.background = 'rgba(0, 0, 0, 0.4)';
                    }
                    
                    // Update face mask color based on detection
                    function updateFaceMask(detected) {
                        if (!faceMask) return;
                        faceMask.classList.toggle('face-detected', detected);
                    }
                    
                    // Real face detection using canvas analysis
                    function startFaceDetection() {
                        var faceDetected = false;
                        var stableCount = 0;
                        var lostCount = 0;
                        var detectionInterval = setInterval(function() {
                            if (!video.videoWidth || !video.videoHeight) return;
                            
                            var ctx = canvas.getContext('2d');
                            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                            
                            var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                            var detected = detectFaceInImageData(imageData);
                            
                            if (detected) {
                                stableCount++;
                                lostCount = 0;
                                if (!faceDetected && stableCount >= detectionSettings.faceStableFrames) {
                                    faceDetected = true;
                                    updateFaceMask(true);
                                    updateStatus('Face detected - Ready to verify', false);
                                    btn.disabled = false;
                                }
                            } else {
                                stableCount = 0;
                                if (faceDetected) {
                                    lostCount++;
                                    if (lostCount >= detectionSettings.faceLostFrames) {
                                        faceDetected = false;
                                        updateFaceMask(false);
                                        updateStatus('Position your face in the guide', false);
                                        btn.disabled = true;
                                    }
                                }
                            }
                        }, detectionSettings.faceDetectionInterval);
                        
                        video.faceDetectionInterval = detectionInterval;
                    }
                    
                    // Simple face detection algorithm
                    function detectFaceInImageData(imageData) {
                        var data = imageData.data;
                        var width = imageData.width;
                        var height = imageData.height;
                        
                        // Define face region (center area where face should be)
                        var centerX = width / 2;
                        var centerY = height / 2;
                        var faceWidth = 200;
                        var faceHeight = 240;
                        
                        var startX = Math.max(0, centerX - faceWidth / 2);
                        var endX = Math.min(width, centerX + faceWidth / 2);
                        var startY = Math.max(0, centerY - faceHeight / 2);
                        var endY = Math.min(height, centerY + faceHeight / 2);
                        
                        var skinPixels = 0;
                        var totalPixels = 0;
                        
                        // Analyze pixels in face region
                        for (var y = startY; y < endY; y += 4) { // Sample every 4th pixel for performance
                            for (var x = startX; x < endX; x += 4) {
                                var i = (y * width + x) * 4;
                                var r = data[i];
                                var g = data[i + 1];
                                var b = data[i + 2];
                                
                                // Simple skin tone detection
                                if (isSkinTone(r, g, b)) {
                                    skinPixels++;
                                }
                                totalPixels++;
                            }
                        }
                        
                        // Face detected if enough skin-tone pixels found
                        var skinRatio = skinPixels / totalPixels;
                        return skinRatio > 0.15; // Threshold for face detection
                    }
                    
                    // Simple skin tone detection
                    function isSkinTone(r, g, b) {
                        // Basic skin tone ranges (covers various skin tones)
                        return (r > 95 && g > 40 && b > 20 && 
                                Math.max(r, g, b) - Math.min(r, g, b) > 15 &&
                                Math.abs(r - g) > 15 && r > g && r > b);
                    }
                    
                    // Calculate motion between two frames (focused on face region)
                    function calculateMotion(frame1, frame2) {
                        var data1 = frame1.data;
                        var data2 = frame2.data;
                        var width = frame1.width;
                        var height = frame1.height;
                        
                        // Focus on center region where face should be
                        var centerX = Math.floor(width / 2);
                        var centerY = Math.floor(height / 2);
                        var regionWidth = Math.floor(width * 0.6);
                        var regionHeight = Math.floor(height * 0.6);
                        
                        var startX = Math.max(0, centerX - Math.floor(regionWidth / 2));
                        var endX = Math.min(width, centerX + Math.floor(regionWidth / 2));
                        var startY = Math.max(0, centerY - Math.floor(regionHeight / 2));
                        var endY = Math.min(height, centerY + Math.floor(regionHeight / 2));
                        
                        var diff = 0;
                        var samples = 0;
                        var significantChanges = 0;
                        
                        // Sample pixels in face region (every 4th pixel for better accuracy)
                        for (var y = startY; y < endY; y += 4) {
                            for (var x = startX; x < endX; x += 4) {
                                var i = (y * width + x) * 4;
                                
                                var r1 = data1[i];
                                var g1 = data1[i + 1];
                                var b1 = data1[i + 2];
                                
                                var r2 = data2[i];
                                var g2 = data2[i + 1];
                                var b2 = data2[i + 2];
                                
                                // Calculate color difference
                                var pixelDiff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
                                diff += pixelDiff;
                                
                                // Count significant pixel changes (threshold: 30)
                                if (pixelDiff > 30) {
                                    significantChanges++;
                                }
                                
                                samples++;
                            }
                        }
                        
                        // Return both average difference and percentage of significant changes
                        var avgDiff = diff / samples;
                        var changePercentage = (significantChanges / samples) * 100;
                        
                        // Motion detected if average diff > 8 AND significant changes > 5%
                        return {
                            avgDiff: avgDiff,
                            changePercentage: changePercentage,
                            hasMotion: avgDiff > detectionSettings.motionDiffThreshold &&
                                       changePercentage > detectionSettings.motionChangeThreshold
                        };
                    }
                    
                    // Start camera
                    function startCamera() {
                        updateStatus('Starting camera...', true);
                        console.log('Requesting camera access...');
                        
                        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                            console.error('getUserMedia not supported');
                            updateStatus('Camera not supported in this browser', false);
                            return;
                        }
                        
                        navigator.mediaDevices.getUserMedia({
                            video: { 
                                facingMode: 'user',
                                width: { ideal: 1280 },
                                height: { ideal: 720 }
                            }
                        })
                        .then(function(s) {
                            console.log('Camera stream obtained:', s);
                            stream = s;
                            video.srcObject = s;
                            
                            video.onloadedmetadata = function() {
                                console.log('Video metadata loaded. Dimensions:', video.videoWidth, 'x', video.videoHeight);
                                canvas.width = video.videoWidth;
                                canvas.height = video.videoHeight;
                                
                                // Show progress steps for liveness detection
                                if (config.active && progressSteps) {
                                    progressSteps.style.display = 'flex';
                                    
                                    // Show/hide challenge steps based on mode
                                    var challengeSteps = progressSteps.querySelectorAll('.challenge-step');
                                    challengeSteps.forEach(function(step) {
                                        step.style.display = config.challenge ? 'flex' : 'none';
                                    });
                                }
                                
                                updateStatus('Position your face in the guide', false);
                                updateStep(1);
                                btn.disabled = false;
                                
                                // Start real face detection
                                startFaceDetection();
                                
                                console.log('Camera started successfully');
                            };
                            
                            video.onerror = function(e) {
                                console.error('Video error:', e);
                                updateStatus('Video error: ' + e.message, false);
                            };
                        })
                        .catch(function(err) {
                            console.error('Camera error:', err);
                            updateStatus('Camera error: ' + err.name + ' - ' + err.message, false);
                        });
                    }
                    
                    // Capture image with face detection and cropping
                    async function captureImage() {
                        console.log('Capturing image from video...');
                        
                        if (!video || !video.videoWidth || !video.videoHeight) {
                            console.error('Video not ready for capture');
                            return null;
                        }
                        
                        try {
                            // Use face detector if available and properly initialized
                            if (faceDetector && faceDetector.isReady && typeof faceDetector.processImage === 'function') {
                                console.log('Using face detector for image capture');
                                try {
                                    var detectedImage = await faceDetector.processImage(video);
                                    if (detectedImage && detectedImage.length > 100) {
                                        console.log('Face detector captured image successfully, size:', detectedImage.length);
                                        return detectedImage;
                                    }
                                    console.warn('Face detector returned invalid image, falling back to simple capture');
                                } catch (detectorError) {
                                    console.warn('Face detector failed, falling back to simple capture:', detectorError);
                                }
                            }
                            
                            // Fallback: simple capture without face detection
                            console.log('Using fallback capture method');
                            var ctx = canvas.getContext('2d');
                            
                            // Ensure canvas is properly sized
                            if (canvas.width !== video.videoWidth || canvas.height !== video.videoHeight) {
                                canvas.width = video.videoWidth;
                                canvas.height = video.videoHeight;
                            }
                            
                            // Draw the current video frame (un-mirrored for correct orientation)
                            ctx.save();
                            ctx.scale(-1, 1);
                            ctx.drawImage(video, -canvas.width, 0, canvas.width, canvas.height);
                            ctx.restore();
                            
                            // Convert to base64 JPEG
                            var imageData = canvas.toDataURL('image/jpeg', 0.95);
                            
                            if (!imageData || imageData.length < 100) {
                                console.error('Captured image is too small or empty');
                                return null;
                            }
                            
                            console.log('Image captured successfully, size:', imageData.length);
                            return imageData;
                        } catch (error) {
                            console.error('Error capturing image:', error);
                            return null;
                        }
                    }
                    
                    // Update button state
                    function updateButton(text, loading) {
                        if (btnText) btnText.textContent = text;
                        if (btnSpinner) btnSpinner.style.display = loading ? 'inline-block' : 'none';
                        btn.disabled = loading;
                    }
                    
                    // Verify function
                    async function verify() {
                        if (capturing) return;
                        capturing = true;
                        
                        updateButton('Processing...', true);
                        updateStatus('Capturing first image...', true);
                        updateStep(2);
                        
                        setTimeout(async function() {
                            var imageData1 = await captureImage();
                            
                            if (!imageData1) {
                                console.error('Failed to capture first image');
                                updateStatus('Failed to capture image. Please try again.', false);
                                capturing = false;
                                updateButton('Verify My Identity', false);
                                updateStep(1);
                                return;
                            }
                            
                            console.log('First image captured, size:', imageData1.length);
                            
                            if (config.active) {
                                // Active liveness detection
                                if (config.challenge) {
                                    // Challenge-response mode
                                    currentChallenge = challengeDirections[Math.floor(Math.random() * challengeDirections.length)];
                                    updateStep(3);
                                    
                                    // Show challenge immediately with clear instruction
                                    showChallenge(currentChallenge);
                                    updateStatus('Turn your head ' + currentChallenge + ' now!', false);
                                    console.log('Challenge shown:', currentChallenge);
                                    
                                    // Start motion detection with baseline
                                    var motionDetected = false;
                                    var previousFrame = null;
                                    var baselineFrame = null;
                                    var frameCount = 0;
                                    var consecutiveMotionFrames = 0;
                                    
                                    var motionCheckInterval = setInterval(function() {
                                        if (motionDetected) return;
                                        
                                        var ctx = canvas.getContext('2d');
                                        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                                        var currentFrame = ctx.getImageData(0, 0, canvas.width, canvas.height);
                                        
                                        frameCount++;
                                        
                                        if (frameCount === 1) {
                                            baselineFrame = currentFrame;
                                            previousFrame = currentFrame;
                                            return;
                                        }

                                        if (!previousFrame || !baselineFrame) {
                                            previousFrame = currentFrame;
                                            return;
                                        }
                                        
                                        var motionFromBaseline = calculateMotion(baselineFrame, currentFrame);
                                        var relativeMotion = calculateMotion(previousFrame, currentFrame);
                                        var motionTriggered = motionFromBaseline.hasMotion || relativeMotion.hasMotion;
                                        
                                        if (motionTriggered && frameCount > detectionSettings.motionMinFramesBeforeDetect) {
                                            consecutiveMotionFrames++;
                                            
                                            if (consecutiveMotionFrames >= detectionSettings.motionConsecutiveFrames) {
                                                motionDetected = true;
                                                clearInterval(motionCheckInterval);
                                                
                                                updateStatus('Perfect! Hold that position...', false);
                                                updateStep(4);
                                                
                                                setTimeout(async function() {
                                                    var imageData2 = await captureImage();
                                                    
                                                    if (!imageData2) {
                                                        console.error('Failed to capture second image');
                                                        updateStatus('Failed to capture second image. Please try again.', false);
                                                        capturing = false;
                                                        updateButton('Verify My Identity', false);
                                                        hideChallenge();
                                                        updateStep(1);
                                                        return;
                                                    }
                                                    
                                                    console.log('Second image captured with challenge:', currentChallenge);
                                                    
                                                    var payload = {
                                                        images: [imageData1, imageData2],
                                                        mode: 'challenge-response',
                                                        challengeDirection: currentChallenge,
                                                        livenessActive: true,
                                                        livenessChallenge: true
                                                    };
                                                    
                                                    input.value = JSON.stringify(payload);
                                                    hideChallenge();
                                                    submitForm();
                                                }, detectionSettings.secondCaptureDelayMs);
                                            }
                                        } else {
                                            consecutiveMotionFrames = Math.max(consecutiveMotionFrames - 1, 0);
                                        }
                                        
                                        previousFrame = currentFrame;
                                    }, detectionSettings.motionCheckInterval);
                                    
                                    // Timeout if no motion detected
                                    setTimeout(function() {
                                        if (!motionDetected) {
                                            clearInterval(motionCheckInterval);
                                            updateStatus('No motion detected. Please try again.', false);
                                            capturing = false;
                                            updateButton('Verify My Identity', false);
                                            hideChallenge();
                                            updateStep(1);
                                        }
                                    }, 10000); // 10 second timeout
                                } else {
                                    // Active liveness without challenge
                                    updateStatus('Move your head slightly...', false);
                                    
                                    setTimeout(async function() {
                                        updateStatus('Capturing second image...', true);
                                        
                                        setTimeout(async function() {
                                            var imageData2 = await captureImage();
                                            console.log('Second image captured for active liveness');
                                            console.log('imageData1 type:', typeof imageData1, 'length:', imageData1 ? imageData1.length : 'null');
                                            console.log('imageData2 type:', typeof imageData2, 'length:', imageData2 ? imageData2.length : 'null');
                                            
                                            if (!imageData2) {
                                                console.error('Failed to capture second image');
                                                updateStatus('Failed to capture second image. Please try again.', false);
                                                capturing = false;
                                                updateButton('Verify My Identity', false);
                                                updateStep(1);
                                                return;
                                            }
                                            
                                            var payload = {
                                                images: [imageData1, imageData2],
                                                mode: 'active',
                                                livenessActive: true,
                                                livenessChallenge: false
                                            };
                                            
                                            console.log('Payload before stringify:', payload);
                                            
                                            input.value = JSON.stringify(payload);
                                            submitForm();
                                        }, 1000);
                                    }, 2000);
                                }
                            } else {
                                // Passive liveness detection (single image)
                                var payload = {
                                    images: [imageData1],
                                    mode: 'passive',
                                    livenessActive: false,
                                    livenessChallenge: false
                                };
                                
                                input.value = JSON.stringify(payload);
                                submitForm();
                            }
                        }, 500);
                    }
                    
                    // Submit form
                    function submitForm() {
                        updateStatus('Verifying your identity...', true, true);
                        updateButton('Verifying...', true);
                        
                        if (stream) {
                            stream.getTracks().forEach(function(track) { track.stop(); });
                        }
                        
                        // Stop face detection
                        if (video.faceDetectionInterval) {
                            clearInterval(video.faceDetectionInterval);
                        }
                        
                        // Add small delay for better UX
                        setTimeout(function() {
                            form.submit();
                        }, 1000);
                    }
                    
                    // Initialize
                    btn.addEventListener('click', verify);
                    startCamera();
                    console.log('Face authentication initialized with config:', config);
                }
            });
        })();
        </script>

        <style>
            .instruction-text {
                text-align: center;
                color: #666;
                margin-bottom: 2rem;
                font-size: 1rem;
            }
            
            .camera-section {
                position: relative;
                width: 100%;
                max-width: 480px;
                margin: 0 auto 2rem;
                border-radius: 12px;
                overflow: hidden;
                background: #000;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
            }
            
            .camera-container {
                position: relative;
                width: 100%;
                aspect-ratio: 4/3;
            }
            
            #video {
                width: 100%;
                height: 100%;
                object-fit: cover;
                display: block;
                /* Mirror video for natural user experience (like looking in a mirror)
                   Note: Captured images are un-mirrored so head movements match actual direction */
                transform: scaleX(-1);
            }
            
            /* Face mask overlay */
            .face-mask {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                z-index: 5;
                pointer-events: none;
            }
            
            .face-outline {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                width: 200px;
                height: 240px;
                border: 2px solid rgba(255, 255, 255, 0.8);
                border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
                transition: border-color 0.3s ease;
            }
            
            .face-mask.face-detected .face-outline {
                border-color: #28a745;
                box-shadow: 0 0 20px rgba(40, 167, 69, 0.5);
            }
            
            .corner {
                position: absolute;
                width: 20px;
                height: 20px;
                border: 3px solid #007bff;
            }
            
            .corner.top-left {
                top: -10px;
                left: -10px;
                border-right: none;
                border-bottom: none;
                border-radius: 8px 0 0 0;
            }
            
            .corner.top-right {
                top: -10px;
                right: -10px;
                border-left: none;
                border-bottom: none;
                border-radius: 0 8px 0 0;
            }
            
            .corner.bottom-left {
                bottom: -10px;
                left: -10px;
                border-right: none;
                border-top: none;
                border-radius: 0 0 0 8px;
            }
            
            .corner.bottom-right {
                bottom: -10px;
                right: -10px;
                border-left: none;
                border-top: none;
                border-radius: 0 0 8px 0;
            }
            
            /* Status overlay */
            .status-overlay {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.3);
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                font-size: 1.1rem;
                text-align: center;
                z-index: 10;
                flex-direction: column;
                gap: 1rem;
                transition: all 0.3s ease;
                pointer-events: none;
            }
            
            .status-overlay.processing {
                background: rgba(0, 0, 0, 0.6);
                backdrop-filter: blur(2px);
            }
            
            #status-text {
                background: rgba(0, 0, 0, 0.75);
                padding: 12px 24px;
                border-radius: 8px;
                font-weight: 600;
                font-size: 1.2rem;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                max-width: 80%;
                line-height: 1.4;
            }
            
            .spinner {
                border: 3px solid rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                border-top: 3px solid #007bff;
                width: 24px;
                height: 24px;
                animation: spin 1s linear infinite;
            }
            
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            
            /* Challenge indicator */
            .challenge-indicator {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                z-index: 15;
                background: linear-gradient(135deg, #007bff, #0056b3);
                padding: 25px;
                border-radius: 50%;
                box-shadow: 0 12px 32px rgba(0, 123, 255, 0.4), 
                           0 0 0 4px rgba(255, 255, 255, 0.9),
                           0 0 0 8px rgba(0, 123, 255, 0.3);
                animation: challengePulse 1.2s ease-in-out infinite;
                border: none;
            }
            
            @keyframes challengePulse {
                0%, 100% { 
                    transform: translate(-50%, -50%) scale(1);
                    box-shadow: 0 12px 32px rgba(0, 123, 255, 0.4), 
                               0 0 0 4px rgba(255, 255, 255, 0.9),
                               0 0 0 8px rgba(0, 123, 255, 0.3),
                               0 0 20px rgba(0, 123, 255, 0.6);
                }
                50% { 
                    transform: translate(-50%, -50%) scale(1.15);
                    box-shadow: 0 16px 40px rgba(0, 123, 255, 0.6), 
                               0 0 0 6px rgba(255, 255, 255, 1),
                               0 0 0 12px rgba(0, 123, 255, 0.5),
                               0 0 30px rgba(0, 123, 255, 0.8);
                }
            }
            
            .challenge-arrow {
                width: 0;
                height: 0;
                border: 18px solid transparent;
                filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
                animation: arrowBounce 1s ease-in-out infinite;
            }
            
            .challenge-arrow.up {
                border-bottom: 28px solid #ffffff;
                border-top: none;
                margin-top: -2px;
            }
            
            .challenge-arrow.down {
                border-top: 28px solid #ffffff;
                border-bottom: none;
                margin-bottom: -2px;
            }
            
            .challenge-arrow.left {
                border-right: 28px solid #ffffff;
                border-left: none;
                margin-left: -2px;
            }
            
            .challenge-arrow.right {
                border-left: 28px solid #ffffff;
                border-right: none;
                margin-right: -2px;
            }
            
            @keyframes arrowBounce {
                0%, 100% { transform: scale(1); }
                50% { transform: scale(1.1); }
            }
            
            /* Direction-specific bounce animations */
            .challenge-arrow.up {
                animation: arrowBounceUp 1s ease-in-out infinite;
            }
            
            .challenge-arrow.down {
                animation: arrowBounceDown 1s ease-in-out infinite;
            }
            
            .challenge-arrow.left {
                animation: arrowBounceLeft 1s ease-in-out infinite;
            }
            
            .challenge-arrow.right {
                animation: arrowBounceRight 1s ease-in-out infinite;
            }
            
            @keyframes arrowBounceUp {
                0%, 100% { transform: translateY(0) scale(1); }
                50% { transform: translateY(-3px) scale(1.1); }
            }
            
            @keyframes arrowBounceDown {
                0%, 100% { transform: translateY(0) scale(1); }
                50% { transform: translateY(3px) scale(1.1); }
            }
            
            @keyframes arrowBounceLeft {
                0%, 100% { transform: translateX(0) scale(1); }
                50% { transform: translateX(-3px) scale(1.1); }
            }
            
            @keyframes arrowBounceRight {
                0%, 100% { transform: translateX(0) scale(1); }
                50% { transform: translateX(3px) scale(1.1); }
            }
            
            /* Challenge text overlay */
            .challenge-text {
                position: absolute;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                z-index: 16;
                background: linear-gradient(135deg, #007bff, #0056b3);
                color: white;
                padding: 14px 28px;
                border-radius: 30px;
                font-weight: 700;
                font-size: 1.3rem;
                box-shadow: 0 8px 24px rgba(0, 123, 255, 0.5),
                           0 0 0 2px rgba(255, 255, 255, 0.9);
                animation: slideDown 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
                letter-spacing: 0.5px;
                text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
            }
            
            @keyframes slideDown {
                from { 
                    opacity: 0; 
                    transform: translateX(-50%) translateY(-20px); 
                }
                to { 
                    opacity: 1; 
                    transform: translateX(-50%) translateY(0); 
                }
            }
            
            /* Controls section */
            .controls-section {
                text-align: center;
                margin-top: 2rem;
            }
            
            .btn {
                padding: 0.75rem 2rem;
                font-size: 1rem;
                border-radius: 8px;
                border: none;
                cursor: pointer;
                transition: all 0.3s ease;
                position: relative;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                gap: 0.5rem;
            }
            
            .btn-primary {
                background: linear-gradient(135deg, #007bff, #0056b3);
                color: white;
                box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
            }
            
            .btn-primary:hover:not(:disabled) {
                background: linear-gradient(135deg, #0056b3, #004085);
                transform: translateY(-2px);
                box-shadow: 0 6px 16px rgba(0, 123, 255, 0.4);
            }
            
            .btn-primary:disabled {
                background: #6c757d;
                cursor: not-allowed;
                transform: none;
                box-shadow: none;
            }
            
            .btn-lg {
                padding: 1rem 2.5rem;
                font-size: 1.1rem;
                min-width: 220px;
                font-weight: 600;
            }
            
            .btn-spinner {
                border: 2px solid rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                border-top: 2px solid white;
                width: 16px;
                height: 16px;
                animation: spin 1s linear infinite;
            }
            
            /* Progress steps */
            .progress-steps {
                display: flex;
                justify-content: center;
                align-items: stretch;
                gap: 0.75rem;
                margin-top: 1.5rem;
                padding: 1.25rem;
                background: rgba(248, 249, 250, 0.95);
                border-radius: 12px;
                backdrop-filter: blur(10px);
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            }
            
            .step {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: flex-start;
                gap: 0.5rem;
                opacity: 0.4;
                transition: all 0.3s ease;
                min-width: 80px;
                flex: 1;
                max-width: 120px;
            }
            
            .step.active {
                opacity: 1;
            }
            
            .step.completed {
                opacity: 1;
            }
            
            .step-circle {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: #e9ecef;
                color: #6c757d;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: bold;
                font-size: 1rem;
                transition: all 0.3s ease;
                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            }
            
            .step.active .step-circle {
                background: linear-gradient(135deg, #007bff, #0056b3);
                color: white;
                transform: scale(1.15);
                box-shadow: 0 4px 12px rgba(0, 123, 255, 0.4);
            }
            
            .step.completed .step-circle {
                background: linear-gradient(135deg, #28a745, #1e7e34);
                color: white;
                box-shadow: 0 2px 8px rgba(40, 167, 69, 0.3);
            }
            
            .step-label {
                font-size: 0.75rem;
                color: #6c757d;
                font-weight: 500;
                text-align: center;
                line-height: 1.2;
                max-width: 100%;
                word-wrap: break-word;
            }
            
            .step.active .step-label {
                color: #007bff;
                font-weight: 700;
            }
            
            .step.completed .step-label {
                color: #28a745;
                font-weight: 600;
            }
            
            /* Responsive design */
            @media (max-width: 768px) {
                .instruction-text {
                    font-size: 0.9rem;
                    margin-bottom: 1.5rem;
                    padding: 0 1rem;
                }
                
                .camera-section {
                    max-width: 100%;
                    margin: 0 0.5rem 1.5rem;
                    border-radius: 8px;
                }
                
                .camera-container {
                    aspect-ratio: 3/4;
                }
                
                .face-outline {
                    width: 140px;
                    height: 180px;
                }
                
                .corner {
                    width: 16px;
                    height: 16px;
                    border-width: 2px;
                }
                
                #status-text {
                    font-size: 1rem;
                    padding: 10px 20px;
                    max-width: 90%;
                }
                
                .challenge-indicator {
                    padding: 20px;
                }
                
                .challenge-arrow {
                    border-width: 14px;
                }
                
                .challenge-arrow.up {
                    border-bottom-width: 22px;
                }
                
                .challenge-arrow.down {
                    border-top-width: 22px;
                }
                
                .challenge-arrow.left {
                    border-right-width: 22px;
                }
                
                .challenge-arrow.right {
                    border-left-width: 22px;
                }
                
                .challenge-text {
                    font-size: 1.1rem;
                    padding: 12px 24px;
                    top: 15px;
                }
                
                .controls-section {
                    margin-top: 1.5rem;
                    padding: 0 0.5rem;
                }
                
                .btn-lg {
                    padding: 0.875rem 2rem;
                    font-size: 1rem;
                    min-width: 180px;
                }
                
                .progress-steps {
                    gap: 0.5rem;
                    padding: 1rem 0.5rem;
                    margin-top: 1rem;
                }
                
                .step {
                    min-width: 60px;
                    max-width: 90px;
                }
                
                .step-label {
                    font-size: 0.65rem;
                }
                
                .step-circle {
                    width: 30px;
                    height: 30px;
                    font-size: 0.85rem;
                }
            }
            
            @media (max-width: 480px) {
                .instruction-text {
                    font-size: 0.85rem;
                    margin-bottom: 1rem;
                }
                
                .camera-section {
                    margin: 0 0 1rem;
                }
                
                .face-outline {
                    width: 120px;
                    height: 160px;
                }
                
                .corner {
                    width: 14px;
                    height: 14px;
                }
                
                #status-text {
                    font-size: 0.9rem;
                    padding: 8px 16px;
                }
                
                .challenge-indicator {
                    padding: 16px;
                }
                
                .challenge-text {
                    font-size: 1rem;
                    padding: 10px 20px;
                    top: 10px;
                }
                
                .btn-lg {
                    padding: 0.75rem 1.5rem;
                    font-size: 0.95rem;
                    min-width: 160px;
                    width: 100%;
                }
                
                .progress-steps {
                    gap: 0.25rem;
                    padding: 0.75rem 0.25rem;
                    flex-wrap: wrap;
                }
                
                .step {
                    min-width: 50px;
                    max-width: 70px;
                }
                
                .step-label {
                    font-size: 0.6rem;
                }
                
                .step-circle {
                    width: 26px;
                    height: 26px;
                    font-size: 0.75rem;
                }
            }
            
            /* Landscape orientation on mobile */
            @media (max-width: 768px) and (orientation: landscape) {
                .camera-section {
                    max-width: 50%;
                    margin: 0 auto 1rem;
                }
                
                .camera-container {
                    aspect-ratio: 4/3;
                }
                
                .progress-steps {
                    flex-direction: row;
                }
            }
            
            /* Animation for face detection */
            @keyframes pulse {
                0% { transform: translate(-50%, -50%) scale(1); }
                50% { transform: translate(-50%, -50%) scale(1.05); }
                100% { transform: translate(-50%, -50%) scale(1); }
            }
            
            .face-mask.face-detected .face-outline {
                animation: pulse 2s ease-in-out infinite;
            }
        </style>
    </#if>
</@layout.registrationLayout>