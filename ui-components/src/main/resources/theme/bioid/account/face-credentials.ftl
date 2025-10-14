<#import "template.ftl" as layout>
<@layout.mainLayout active='face-credentials' bodyClass='face-credentials'; section>

<div class="row">
    <div class="col-md-10">
        <h2>${msg("faceCredentialsTitle")}</h2>
    </div>
</div>

<!-- Enhanced Face Credentials Management Component -->
<div id="face-credentials-container">
    <!-- Content will be dynamically loaded by JavaScript -->
    <div class="loading">Loading face authentication data...</div>
</div>

<!-- Include JavaScript and CSS -->
<script>
    // Global variables for the JavaScript component
    var keycloakRealm = '${realm.name}';
    var keycloakToken = '${accessToken!""}';
</script>

<script src="${url.resourcesPath}/js/template-status.js"></script>
<script src="${url.resourcesPath}/js/face-credentials.js"></script>
<link rel="stylesheet" href="${url.resourcesPath}/css/account.css">

</@layout.mainLayout>