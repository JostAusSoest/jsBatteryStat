import static java.lang.Math.*


definition name:'jsBatteryStat', namespace:'de.schwider', author:'Jost Schwider', description:'Just Simple Battery Statistics', iconUrl:'', iconX2Url:''


preferences()
{
  page name:'pageConfig'
}


def pageConfig()
{
  if (! state.accessToken) createAccessToken()
  
  dynamicPage(name:'pageConfig', uninstall:true, install:true)
  {
    section('<div style="margin-top:-8ex" />')
    {
      String path = "/batteryStat/MODE?access_token=$state.accessToken"
      paragraph htmlURL('Local Webhook-URL', getFullLocalApiServerUrl() + path)
      paragraph htmlURL('Cloud Webhook-URL', getFullApiServerUrl() + path)
      paragraph '<p>Replace "<tt>MODE</tt>" with "<tt>name</tt>" or "<tt>room</tt>" to change sorting.</p>'
    }
    
    section('<div style="margin-top:-2ex" />')
    {
      input 'batteryDevices', 'capability.battery', title:'Battery Devices:', multiple:true, required:true
    }
  }
}


mappings
{
  path '/batteryStat/:mode', { action:[GET:'batteryStat'] }
  path '/batteryStat/', { action:[GET:'batteryStat'] }
  path '/batteryStat', { action:[GET:'batteryStat'] }
}


def batteryStat()
{
  String mode = URLDecoder.decode(params.mode ?: '').trim().toLowerCase()
  
  def devices
  switch (mode)
  {
    case 'name': devices = batteryDevices.sort { it.displayName }; break
    case 'room': devices = batteryDevices.sort { (it.properties.roomName ?: '') + '>' + (100 + it.currentBattery) + '>' + it.displayName }; break
    default:     devices = batteryDevices.sort { (100 + it.currentBattery) + '>' + it.displayName }
  }
  
  String html = """<!DOCTYPE html>
<html>
<head>
<title>Battery Stat: $mode</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="author" content="Jost Schwider">
</head>
<style>
h1, h2 {margin:0; padding:0}
h2 {margin-top:1ex}
a {text-decoration:none; color:black}
div {border:1px solid #000}
</style>
<body>
<h1>Battery Stat</h1>
"""
  String name
  String room
  String label
  String color
  for (def device in devices)
  {
    if (mode == 'room' && room != device.roomName) html += "<h2>$device.roomName:</h2>"
    
    int battery = (device.currentBattery as Float).round()
    int red   = min(100, 2 * (100 - battery))
    int green = min(100, 2 * battery)
    name  = device.displayName
    room  = device.roomName
    label = (mode == 'room' || room == null ? name : "$name ($room)")
    label = "<a href='/device/edit/$device.id' target=_blank>$label</a>"
    color = "rgb($red%,$green%,0%)"
    html += "<div style='background:linear-gradient(to right,$color,$color $battery%,#fff $battery%)'>$label: $battery%</div>\n"
  }
  html += '</body>\n</html>'
  render contentType:'text/html', data:html, status:200
}


void installed()
{
  log.info 'installed'
  initialize ()
}


void updated()
{
  log.info 'updated'
  initialize ()
}


void uninstalled()
{
  log.info 'uninstalled'
  unsubscribe ()
  unschedule ()
}


void initialize()
{
  unsubscribe ()
  unschedule ()
}


/** HELPER: **/


String htmlURL(String title, String url)
{
  return "<strong>$title:</strong><p style='margin:0;background:#eee;padding:1ex;line-height:1.1rem'><a href='$url' target='_blank'>$url</a></p>"
}
