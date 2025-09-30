<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <h1 id="kc-page-title">${msg("faceAuthTitle")}</h1>
        <p class="subtitle">${msg("faceAuthDescription")}</p>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if message?has_content && (message.type = 'error')>
                    <div class="alert alert-error" role="alert">
                        <span class="pficon pficon-error-circle-o"></span>
                        <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc} (${retryCount} / ${maxRetries})</span>
                    </div>
                </#if>

                <div id="verification-ui">
                    <div class="camera-section">
                        <video id="video" autoplay muted playsinline aria-label="Camera feed for face verification"></video>
                        <canvas id="canvas" style="display:none;"></canvas>
                        <div class="face-guide-overlay">
                            <svg id="face-guide-svg" class="face-guide" viewBox="0 0 300 400">
                                <path d="M150,50 A120,150 0 0,1 150,350 A120,150 0 0,1 150,50 Z" stroke-width="4" stroke-dasharray="10 5" fill="none"/>
                            </svg>
                        </div>
                        <div id="status-overlay" class="status-overlay" style="display:none;">
                            <div id="spinner" class="spinner"></div>
                            <p id="status-text"></p>
                        </div>
                    </div>
                    
                    <div class="controls-section">
                        <form id="face-auth-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="imageData" id="image-data-input" />
                            <button id="verify-btn" type="button" class="btn btn-primary btn-lg" disabled>
                                ${msg("verifyIdentity")}
                            </button>
                        </form>
                        
                        <#if retryCount?? && (retryCount?number > 0)>
                            <div class="fallback-section">
                                <p>${msg("havingTrouble")}</p>
                                <a href="${url.loginAction}?execution=${execution.id}&client_id=${client.clientId}&tab_id=${tabId}&auth_session_id=${authSessionId}&action=fallback" class="fallback-link">
                                    ${msg("useAlternativeMethod")}
                                </a>
                            </div>
                        </#if>
                    </div>
                </div>
            </div>
        </div>

        <script nonce="${cspNonce!}">
            (function() {
                const video = document.getElementById('video');
                const canvas = document.getElementById('canvas');
                const verifyBtn = document.getElementById('verify-btn');
                const imageDataInput = document.getElementById('image-data-input');
                const authForm = document.getElementById('face-auth-form');
                const statusOverlay = document.getElementById('status-overlay');
                const statusText = document.getElementById('status-text');
                const spinner = document.getElementById('spinner');
                let stream;

                async function startCamera() {
                    try {
                        stream = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' } });
                        video.srcObject = stream;
                        video.onloadedmetadata = () => {
                            canvas.width = video.videoWidth;
                            canvas.height = video.videoHeight;
                            verifyBtn.disabled = false;
                        };
                    } catch (err) {
                        console.error("Camera error:", err);
                        statusOverlay.style.display = 'flex';
                        spinner.style.display = 'none';
                        statusText.textContent = "Camera access denied. Please check browser permissions.";
                    }
                }

                function stopCamera() {
                    if (stream) {
                        stream.getTracks().forEach(track => track.stop());
                    }
                }

                function verify() {
                    verifyBtn.disabled = true;
                    statusOverlay.style.display = 'flex';
                    spinner.style.display = 'block';
                    statusText.textContent = "${msg('verifyingIdentity')}";

                    // Add a slight delay for user feedback before capturing
                    setTimeout(() => {
                        canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
                        imageDataInput.value = canvas.toDataURL('image/jpeg', 0.9);
                        stopCamera();
                        authForm.submit();
                    }, 500);
                }

                verifyBtn.addEventListener('click', verify);
                window.addEventListener('beforeunload', stopCamera);
                startCamera();
            })();
        </script>
        
        <style>
            /* Main layout - reduced spacing */
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
                padding: 1.5rem;
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
            
            /* Camera section - consistent with enrollment */
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
                filter: drop-shadow(0 0 4px rgba(255,255,255,0.3));
            }
            
            @keyframes pulse {
                0%, 100% { opacity: 0.6; }
                50% { opacity: 1; }
            }
            
            .status-overlay {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0,0,0,0.8);
                color: white;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                text-align: center;
                padding: 1.5rem;
            }
            
            .spinner {
                border: 4px solid rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                border-top: 4px solid #fff;
                width: 48px;
                height: 48px;
                animation: spin 1s linear infinite;
                margin-bottom: 1rem;
            }
            
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            
            /* Controls section */
            .controls-section {
                text-align: center;
            }
            
            .btn-lg {
                padding: 0.875rem 2rem;
                font-size: 1.1rem;
                font-weight: 600;
                border-radius: 6px;
                transition: all 0.2s;
                min-width: 180px;
                background-color: #007bff;
                border-color: #007bff;
            }
            
            .btn-lg:hover:not(:disabled) {
                background-color: #0056b3;
                border-color: #0056b3;
                transform: translateY(-1px);
            }
            
            .btn-lg:disabled {
                opacity: 0.6;
                cursor: not-allowed;
            }
            
            /* Fallback section */
            .fallback-section {
                margin-top: 1.5rem;
                padding-top: 1rem;
                border-top: 1px solid #e9ecef;
                font-size: 0.9rem;
                color: #6c757d;
            }
            
            .fallback-section p {
                margin-bottom: 0.5rem;
            }
            
            .fallback-link {
                color: #007bff;
                text-decoration: none;
                font-weight: 500;
            }
            
            .fallback-link:hover {
                color: #0056b3;
                text-decoration: underline;
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
                
                #kc-form-wrapper {
                    padding: 1rem;
                }
                
                .camera-section {
                    margin-bottom: 1rem;
                }
                
                .btn-lg {
                    padding: 0.75rem 1.5rem;
                    font-size: 1rem;
                    min-width: 160px;
                }
            }
        </style>
    </#if>
</@layout.registrationLayout>