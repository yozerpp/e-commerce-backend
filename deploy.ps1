if($args.Count -eq 0){
Write-Output "Provide path to the file."
return
}
$file=Get-Item -Path $args[0]
$outputPath=$args[1]
$config= @{
Method= 'POST'
Uri= "https://file.io/"
Headers= @{
ContentType='multipart/form'
}
Form= @{
file= $file
expires= '10m'
maxDownloads=1
autoDelete=$true
}
}
$resp=Invoke-RestMethod @config
if ($null -eq $resp){
    Write-Output "Error uploading file... exiting."
    exit
}
Write-Output "uploaded the file"
$arg=$resp.link
ssh root@213.142.157.170 -p 22 "./redeploy.sh $arg $outputPath"