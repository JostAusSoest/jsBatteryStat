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
  
  String html = """
<!DOCTYPE html>
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
</style>
<body>

<h1>Battery Stat: $mode</h1>
"""
  String name
  String room
  for (def device in devices)
  {
    if (mode == 'room' && room != device.roomName) html += "<h2>$device.roomName:</h2>"
    name = device.displayName
    room = device.roomName
    int battery = (device.currentBattery as Float).round()
    int r = min(100, 2 * (100 - battery))
    int g = min(100, 2 * battery)
    html += "<div style='border:1px solid #000'><div style='background:rgb($r%,$g%,0%);width:$battery%'>$name ($room): $battery%</div></div>\n"
  }
  html += '</body></html>'
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
