#!/bin/bash
curl -X PUT --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'X-DEVOPS-UID: admin' -d "{    
   \"showProjectList\": true,   
   \"showNav\": true,   
   \"status\": \"ok\",   
   \"deleted\": false,          
   \"link\": \"/codecc/0\",                   
   \"linkNew\": \"/codecc/0\",   
   \"iframeUrl\": \"$BK_CODECC_URL\"  
 }" "http://$BK_CI_PROJECT_INNER_URL/api/op/services/update/CodeCC"