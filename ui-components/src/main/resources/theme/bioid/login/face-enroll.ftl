<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <h1 id="kc-page-title">${msg("face-enroll.title")}</h1>
        <p class="subtitle">${msg("face-enroll.subtitle")}</p>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if message?has_content && (message.type = 'error')>
                    <div class="alert alert-error" role="alert">
                        <span class="pficon pficon-error-circle-o"></span>
                        <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>

                <div id="enrollment-ui">
                    <div class="camera-section">
                        <video id="video" autoplay muted playsinline aria-label="${msg('face-enroll.camera.video.label')}"></video>
                        <canvas id="canvas" style="display:none;"></canvas>
                        <div class="face-guide-overlay">
                            <svg id="face-guide-svg" class="face-guide" viewBox="0 0 300 400">
                                <path d="M150,50 A120,150 0 0,1 150,350 A120,150 0 0,1 150,50 Z" stroke-width="4" stroke-dasharray="10 5" fill="none"/>
                            </svg>
                        </div>
                        <div id="camera-error" class="camera-error-overlay" style="display:none;">
                            <h3>${msg("face-enroll.camera.error.title")}</h3>
                            <p>${msg("face-enroll.camera.error.message")}</p>
                            <button type="button" id="camera-retry-btn" class="btn btn-primary">${msg("face-enroll.camera.retry")}</button>
                        </div>
                    </div>
                    
                    <div class="progress-section">
                        <div id="progress-indicator" class="progress-indicator">
                            <div class="step" data-step="1">1</div>
                            <div class="connector"></div>
                            <div class="step" data-step="2">2</div>
                            <div class="connector"></div>
                            <div class="step" data-step="3">3</div>
                        </div>
                        <p id="instruction-text" class="instruction-text" aria-live="polite">${msg("face-enroll.instruction.first")}</p>
                    </div>
                    
                    <div class="controls-section">
                        <form id="face-enroll-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="action" value="submit" />
                            <input type="hidden" name="imageData" id="image-data-input" />
                            <div class="button-group">
                                <button id="capture-btn" type="button" class="btn btn-primary btn-lg" disabled>
                                    ${msg("face-enroll.button.capture")}
                                </button>
                                <button id="submit-btn" type="submit" class="btn btn-success btn-lg" style="display:none;">
                                    ${msg("face-enroll.button.submit")}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
                
                <div class="security-notice">
                    <p><strong>${msg("face-enroll.security.title")}</strong>: ${msg("face-enroll.security.description")}</p>
                </div>
            </div>
        </div>

        <script nonce="${cspNonce!}">
            (function() {
                const video = document.getElementById('video');
                const canvas = document.getElementById('canvas');
                const captureBtn = document.getElementById('capture-btn');
                const submitBtn = document.getElementById('submit-btn');
                const imageDataInput = document.getElementById('image-data-input');
                const instructionText = document.getElementById('instruction-text');
                const progressIndicator = document.getElementById('progress-indicator');
                const cameraError = document.getElementById('camera-error');
                const cameraRetryBtn = document.getElementById('camera-retry-btn');
                const faceGuideSvg = document.getElementById('face-guide-svg');
                
                let capturedFrames = 0;
                const requiredFrames = ${minRequiredFrames!"3"};
                let stream;
                const capturedImages = [];

                const instructions = [
                    "${msg('face-enroll.instruction.first')}",
                    "${msg('face-enroll.instruction.second')}",
                    "${msg('face-enroll.instruction.third')}",
                    "${msg('face-enroll.instruction.complete')}"
                ];

                async function startCamera() {
                    try {
                        cameraError.style.display = 'none';
                        stream = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' } });
                        video.srcObject = stream;
                        video.onloadedmetadata = () => {
                            canvas.width = video.videoWidth;
                            canvas.height = video.videoHeight;
                            captureBtn.disabled = false;
                            updateProgress();
                        };
                    } catch (err) {
                        console.error("Camera error:", err);
                        instructionText.textContent = err.message;
                        cameraError.style.display = 'flex';
                    }
                }

                function stopCamera() {
                    if (stream) {
                        stream.getTracks().forEach(track => track.stop());
                    }
                }

                function updateProgress() {
                    instructionText.textContent = instructions[capturedFrames];
                    const steps = progressIndicator.querySelectorAll('.step');
                    steps.forEach((step, index) => {
                        step.classList.remove('active', 'completed');
                        if (index < capturedFrames) {
                            step.classList.add('completed');
                        } else if (index === capturedFrames) {
                            step.classList.add('active');
                        }
                    });

                    if (capturedFrames >= requiredFrames) {
                        captureBtn.style.display = 'none';
                        submitBtn.style.display = 'block';
                        faceGuideSvg.classList.add('completed');
                    } else {
                        faceGuideSvg.classList.remove('completed');
                    }
                }

                function captureFrame() {
                    if (capturedFrames >= requiredFrames) return;
                    
                    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
                    const imageData = canvas.toDataURL('image/jpeg', 0.9);
                    
                    capturedImages.push(imageData);
                    // For a simplified flow, we'll just submit the last good image.
                    // A more advanced implementation would send all images.
                    imageDataInput.value = imageData; 
                    
                    capturedFrames++;
                    updateProgress();
                }

                // Prevent double submission
                let isSubmitting = false;
                const form = document.getElementById('face-enroll-form');
                form.addEventListener('submit', function(e) {
                    if (isSubmitting) {
                        e.preventDefault();
                        return false;
                    }
                    isSubmitting = true;
                    submitBtn.disabled = true;
                    submitBtn.textContent = "${msg('face-enroll.button.processing')}";
                    stopCamera();
                });

                captureBtn.addEventListener('click', captureFrame);
                cameraRetryBtn.addEventListener('click', startCamera);
                window.addEventListener('beforeunload', stopCamera);
                startCamera();
            })();
        </script>

        <style>
            /* Main layout - reduced top/bottom spacing */
            #kc-form {
                max-width: 500px;
                margin: 0 auto;
                padding: 1rem;
            }
            
            #kc-form-wrapper {
                background: #fff;
                border-radius: 8px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                overflow: hidden;
            }
            
            /* Header styling */
            #kc-page-title {
                text-align: center;
                font-size: 1.5rem;
                margin-bottom: 0.5rem;
                color: #333;
                font-weight: 600;
            }
            
            .subtitle {
                text-align: center;
                color: #666;
                margin: 0 0 1.5rem 0;
                font-size: 0.95rem;
            }
            
            /* Camera section - consistent with face mask */
            .camera-section {
                position: relative;
                width: 100%;
                aspect-ratio: 4 / 3;
                background: #1a1a1a;
                border-radius: 8px;
                overflow: hidden;
                margin-bottom: 1.5rem;
                border: 2px solid #e0e0e0;
            }
            
            #video {
                width: 100%;
                height: 100%;
                object-fit: cover;
                transform: scaleX(-1);
            }
            
            .face-guide-overlay {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
                pointer-events: none;
            }
            
            .face-guide {
                width: 55%;
                height: 75%;
                stroke: rgba(255,255,255,0.8);
                stroke-width: 3;
                animation: pulse 2s infinite ease-in-out;
                transition: stroke 0.3s;
                filter: drop-shadow(0 0 4px rgba(255,255,255,0.3));
            }
            
            .face-guide.completed {
                stroke: #28a745;
                animation: none;
                filter: drop-shadow(0 0 6px rgba(40,167,69,0.5));
            }
            
            @keyframes pulse {
                0%, 100% { opacity: 0.6; }
                50% { opacity: 1; }
            }
            
            .camera-error-overlay {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0,0,0,0.85);
                color: white;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                text-align: center;
                padding: 1.5rem;
            }
            
            /* Progress section */
            .progress-section {
                margin-bottom: 1.5rem;
                text-align: center;
                padding: 0 1rem;
            }
            
            .progress-indicator {
                display: flex;
                justify-content: center;
                align-items: center;
                margin-bottom: 1rem;
                max-width: 200px;
                margin-left: auto;
                margin-right: auto;
            }
            
            .step {
                width: 32px;
                height: 32px;
                border-radius: 50%;
                background: #e0e0e0;
                color: #666;
                display: flex;
                justify-content: center;
                align-items: center;
                font-weight: bold;
                transition: all 0.3s;
                border: 2px solid #e0e0e0;
                font-size: 0.9rem;
            }
            
            .step.active {
                background: #007bff;
                border-color: #007bff;
                color: white;
                transform: scale(1.1);
            }
            
            .step.completed {
                background: #28a745;
                border-color: #28a745;
                color: white;
            }
            
            .connector {
                flex: 1;
                height: 3px;
                background: #e0e0e0;
                margin: 0 8px;
                border-radius: 2px;
            }
            
            .instruction-text {
                font-size: 1rem;
                color: #333;
                min-height: 2.5em;
                display: flex;
                align-items: center;
                justify-content: center;
                line-height: 1.4;
                font-weight: 500;
            }
            
            /* Controls section - centered button */
            .controls-section {
                text-align: center;
                padding: 0 1rem 1.5rem 1rem;
            }
            
            .button-group {
                display: flex;
                justify-content: center;
                gap: 1rem;
            }
            
            .btn-lg {
                padding: 0.875rem 2rem;
                font-size: 1.1rem;
                font-weight: 600;
                border-radius: 6px;
                transition: all 0.2s;
                min-width: 160px;
            }
            
            .btn-primary {
                background-color: #007bff;
                border-color: #007bff;
            }
            
            .btn-primary:hover:not(:disabled) {
                background-color: #0056b3;
                border-color: #0056b3;
                transform: translateY(-1px);
            }
            
            .btn-success {
                background-color: #28a745;
                border-color: #28a745;
            }
            
            .btn-success:hover {
                background-color: #1e7e34;
                border-color: #1e7e34;
                transform: translateY(-1px);
            }
            
            /* Security notice */
            .security-notice {
                background: #f8f9fa;
                padding: 1rem 1.5rem;
                border-top: 1px solid #e9ecef;
                font-size: 0.85rem;
                color: #6c757d;
                text-align: center;
            }
            
            /* Alert styling */
            .alert-error {
                margin-bottom: 1.5rem;
                padding: 1rem;
                background-color: #f8d7da;
                border: 1px solid #f5c6cb;
                border-radius: 6px;
                color: #721c24;
            }
            
            .alert-error .pficon {
                margin-right: 0.5rem;
            }
            
            /* Responsive adjustments */
            @media (max-width: 576px) {
                #kc-form {
                    padding: 0.5rem;
                }
                
                .camera-section {
                    margin-bottom: 1rem;
                }
                
                .btn-lg {
                    padding: 0.75rem 1.5rem;
                    font-size: 1rem;
                    min-width: 140px;
                }
                
                .button-group {
                    flex-direction: column;
                    align-items: center;
                }
            }
        </style>
    </#if>
</@layout.registrationLayout>