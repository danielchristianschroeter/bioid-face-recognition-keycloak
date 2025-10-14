<#import "template.ftl" as layout>
<@layout.mainLayout active='face-credentials' bodyClass='template-status-info'; section>

<div class="row">
    <div class="col-md-10">
        <h2>Understanding Your Face Template Status</h2>
    </div>
</div>

<div class="template-info-section">
    <div class="info-card">
        <h3>What is a Face Template?</h3>
        <p>A face template is a mathematical representation of your facial features used for authentication. It's not a photo or image, but rather a set of numerical values that uniquely identify your face.</p>
        
        <div class="info-highlight">
            <strong>Privacy Note:</strong> Your face template cannot be reverse-engineered to recreate your face or used for any purpose other than authentication.
        </div>
    </div>

    <div class="info-card">
        <h3>Template Status Meanings</h3>
        
        <div class="status-explanation">
            <div class="status-item">
                <div class="status-icon healthy">✅</div>
                <div class="status-content">
                    <h4>Excellent (Healthy)</h4>
                    <p>Your template is in perfect condition, using the latest technology and providing optimal security and accuracy.</p>
                </div>
            </div>
            
            <div class="status-item">
                <div class="status-icon warning">⚠️</div>
                <div class="status-content">
                    <h4>Good (Upgrade Available)</h4>
                    <p>Your template works well but can benefit from an upgrade to the latest technology for improved performance.</p>
                </div>
            </div>
            
            <div class="status-item">
                <div class="status-icon attention">⚠️</div>
                <div class="status-content">
                    <h4>Needs Attention</h4>
                    <p>Your template may have quality issues that could affect authentication reliability. Consider re-enrolling.</p>
                </div>
            </div>
        </div>
    </div>

    <div class="info-card">
        <h3>Technical Information</h3>
        
        <div class="tech-info">
            <div class="tech-item">
                <h4>Encoder Version</h4>
                <p>This indicates which version of the face recognition algorithm was used to create your template. Newer versions provide better accuracy and security.</p>
            </div>
            
            <div class="tech-item">
                <h4>Feature Vectors</h4>
                <p>The number of unique facial features captured in your template. More feature vectors generally mean better authentication accuracy:</p>
                <ul>
                    <li><strong>20+:</strong> Excellent quality - highly accurate in all conditions</li>
                    <li><strong>15-19:</strong> Good quality - reliable in most conditions</li>
                    <li><strong>10-14:</strong> Fair quality - may have reduced accuracy in poor lighting</li>
                    <li><strong>Below 10:</strong> Poor quality - consider re-enrolling</li>
                </ul>
            </div>
        </div>
    </div>

    <div class="info-card">
        <h3>Template Upgrades</h3>
        <p>Template upgrades improve your authentication experience without requiring you to re-enroll your face. The process:</p>
        
        <div class="upgrade-process">
            <div class="process-step">
                <div class="step-number">1</div>
                <div class="step-content">
                    <h4>Automatic Detection</h4>
                    <p>The system identifies templates that can benefit from upgrades</p>
                </div>
            </div>
            
            <div class="process-step">
                <div class="step-number">2</div>
                <div class="step-content">
                    <h4>Background Processing</h4>
                    <p>Your template is upgraded using advanced algorithms</p>
                </div>
            </div>
            
            <div class="process-step">
                <div class="step-number">3</div>
                <div class="step-content">
                    <h4>Seamless Transition</h4>
                    <p>You continue using face authentication without interruption</p>
                </div>
            </div>
        </div>
        
        <div class="upgrade-benefits">
            <h4>Benefits of Upgrading:</h4>
            <ul>
                <li>Improved accuracy in various lighting conditions</li>
                <li>Enhanced security against spoofing attempts</li>
                <li>Better performance and faster authentication</li>
                <li>Future compatibility with system updates</li>
            </ul>
        </div>
    </div>

    <div class="info-card">
        <h3>Privacy and Security</h3>
        <div class="privacy-info">
            <div class="privacy-item">
                <h4>🔒 Data Protection</h4>
                <p>Your face template is encrypted and stored securely. It cannot be used to reconstruct your face or identify you in photos.</p>
            </div>
            
            <div class="privacy-item">
                <h4>🗑️ Data Control</h4>
                <p>You have full control over your biometric data. You can delete your template at any time through your account settings.</p>
            </div>
            
            <div class="privacy-item">
                <h4>📊 Usage Tracking</h4>
                <p>We only track when you last used face authentication, not detailed usage patterns or location information.</p>
            </div>
            
            <div class="privacy-item">
                <h4>🌍 GDPR Compliance</h4>
                <p>Our system is fully GDPR compliant. You can export or delete your data at any time.</p>
            </div>
        </div>
    </div>

    <div class="info-card">
        <h3>Troubleshooting</h3>
        <div class="troubleshooting">
            <div class="trouble-item">
                <h4>Authentication Fails Frequently</h4>
                <ul>
                    <li>Check if your template status shows "Needs Attention"</li>
                    <li>Ensure good lighting when authenticating</li>
                    <li>Consider re-enrolling if the template quality is poor</li>
                </ul>
            </div>
            
            <div class="trouble-item">
                <h4>Template Status Shows "Unknown"</h4>
                <ul>
                    <li>Refresh the page to reload status information</li>
                    <li>This is usually temporary and doesn't affect authentication</li>
                    <li>Contact support if the status remains unknown for extended periods</li>
                </ul>
            </div>
            
            <div class="trouble-item">
                <h4>Want to Improve Template Quality</h4>
                <ul>
                    <li>Re-enroll in good, even lighting</li>
                    <li>Ensure your camera is clean and positioned properly</li>
                    <li>Follow the enrollment instructions carefully</li>
                </ul>
            </div>
        </div>
    </div>
</div>

<div class="action-section">
    <a href="${url.accountUrl}/face-credentials" class="btn btn-primary">Back to Face Authentication</a>
</div>

<style>
.template-info-section {
    max-width: 800px;
    margin: 0 auto;
}

.info-card {
    background-color: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
}

.info-card h3 {
    color: #333;
    margin-top: 0;
    margin-bottom: 1rem;
    border-bottom: 2px solid #0066cc;
    padding-bottom: 0.5rem;
}

.info-highlight {
    background-color: #e7f3ff;
    border: 1px solid #b8daff;
    border-radius: 6px;
    padding: 1rem;
    margin: 1rem 0;
    color: #004085;
}

.status-explanation {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.status-item {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
    padding: 1rem;
    background-color: #f8f9fa;
    border-radius: 6px;
}

.status-icon {
    font-size: 1.5rem;
    line-height: 1;
}

.status-content h4 {
    margin: 0 0 0.5rem 0;
    color: #333;
}

.status-content p {
    margin: 0;
    color: #666;
}

.tech-info {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.tech-item h4 {
    color: #333;
    margin-bottom: 0.5rem;
}

.tech-item p, .tech-item ul {
    color: #666;
}

.upgrade-process {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin: 1rem 0;
}

.process-step {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
}

.step-number {
    background-color: #0066cc;
    color: white;
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    flex-shrink: 0;
}

.step-content h4 {
    margin: 0 0 0.5rem 0;
    color: #333;
}

.step-content p {
    margin: 0;
    color: #666;
}

.upgrade-benefits {
    background-color: #f8f9fa;
    padding: 1rem;
    border-radius: 6px;
    margin-top: 1rem;
}

.upgrade-benefits h4 {
    color: #333;
    margin-top: 0;
}

.privacy-info {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1rem;
}

.privacy-item {
    background-color: #f8f9fa;
    padding: 1rem;
    border-radius: 6px;
    border-left: 4px solid #0066cc;
}

.privacy-item h4 {
    margin: 0 0 0.5rem 0;
    color: #333;
}

.privacy-item p {
    margin: 0;
    color: #666;
    font-size: 0.9rem;
}

.troubleshooting {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.trouble-item {
    background-color: #f8f9fa;
    padding: 1rem;
    border-radius: 6px;
}

.trouble-item h4 {
    color: #333;
    margin: 0 0 0.5rem 0;
}

.trouble-item ul {
    margin: 0;
    color: #666;
}

.action-section {
    text-align: center;
    margin: 2rem 0;
}

@media (max-width: 768px) {
    .status-item {
        flex-direction: column;
        text-align: center;
    }
    
    .process-step {
        flex-direction: column;
        text-align: center;
    }
    
    .privacy-info {
        grid-template-columns: 1fr;
    }
}
</style>

</@layout.mainLayout>