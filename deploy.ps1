if($args.Count -eq 0){
Write-Output "Provide path to the file."
return
}
$file=Get-Item -Path $args[0]
$outputPath=$args[1]
Write-Output "$outputPath"
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
ssh root@185.87.252.50 -p 22666 "./redeploy.sh $arg $outputPath"