import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.NearbySearchRequest
import com.google.maps.model.PlaceType
import org.json.JSONObject
import io.javalin.Javalin
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


fun ejer1(resquest: String): String{
    val obj = JSONObject(resquest)
    val origen = obj["origen"].toString()
    val destino= obj["destino"].toString()
    val api_key = "AIzaSyAlXq_VP82-PR8eLsemhkRV2CUby8inPmE"
    val context = GeoApiContext().setApiKey(api_key)
    val result = DirectionsApi.getDirections(context, origen, destino).await()
    var x = 0
    var jsonString = "{\n\t\"ruta\": ["
    while (x < result.routes[0].legs[0].steps.size){
        var lat = result.routes[0].legs[0].steps[x].startLocation.lat
        var lng = result.routes[0].legs[0].steps[x].startLocation.lng
        jsonString += "\n\t\t{\n\t\t\"lat\": ${lat},\n\t\t\"lng\": ${lng}\n\t\t},"
        x++
    }
    jsonString = jsonString.substring(0,jsonString.length-1)
    jsonString += "\n\t]\n}"
    return jsonString
}

fun ejer2(resquest: String): String{
    val obj = JSONObject(resquest)
    val origen = obj["origen"].toString()
    val api_key = "AIzaSyAlXq_VP82-PR8eLsemhkRV2CUby8inPmE"

    val context = GeoApiContext().setApiKey(api_key)
    val results = GeocodingApi.geocode(context, origen).await()

    val request = NearbySearchRequest(context)
    request.location(results[0].geometry.location)
    request.radius(500)
    request.type(PlaceType.RESTAURANT)
    val response =  request.await()

    var jsonString = "{\n\t\"restaurantes\": ["
    var x = 0
    while (x<response.results.size){
        var name = response.results[x].name
        var lat = response.results[x].geometry.location.lat
        var lng = response.results[x].geometry.location.lng
        jsonString += "\n\t\t{\n\t\t\t\"nombre\": \"${name}\",\n\t\t\t\"lat\": ${lat},\n\t\t\t\"lng\": ${lng}\n\t\t},"
        x++
    }
    jsonString = jsonString.substring(0,jsonString.length-1)
    jsonString += "\n\t]\n}"
    return jsonString
}

fun ejer3(resquest: String): String{
    val obj = JSONObject(resquest)
    val nom = obj["nombre"].toString()
    val data= obj["data"].toString()

    var image: BufferedImage? = null
    val imageByte: ByteArray

    val decoder = BASE64Decoder()
    imageByte = decoder.decodeBuffer(data)
    val bis = ByteArrayInputStream(imageByte)
    image = ImageIO.read(bis)
    bis.close()

    val outputfile = File(nom)
    ImageIO.write(image!!, "bmp", outputfile)

    val imageRead = ImageIO.read(outputfile)
    val w = imageRead.getWidth()
    val h = imageRead.getHeight()

    var x = 0
    while (x < w){
        var y = 0
        while (y < h){
            var color = Color(imageRead.getRGB(x,y))
            var red = (color.red * 0.299).toInt()
            var green = (color.green * 0.587).toInt()
            var blue = (color.blue * 0.114).toInt()
            var gray = red + green + blue
            var newColor = Color(gray,gray,gray)
            imageRead.setRGB(x,y,newColor.rgb)
            y++
        }
        x++
    }

    val finalFile = File("grayscale_"+nom)
    ImageIO.write(imageRead,"bmp",finalFile)

    var imageString: String? = null
    val bos = ByteArrayOutputStream()

    try {
        ImageIO.write(imageRead, "bmp", bos)
        val imageBytes = bos.toByteArray()

        val encoder = BASE64Encoder()
        imageString = encoder.encode(imageBytes)

        bos.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val jsonString = "{\n\t\"nombre\": \"gris_${nom}\",\n\t\"data\": \"${imageString}\"\n}"
    return jsonString
}

fun ejer4(resquest: String): String{
    val obj = JSONObject(resquest)
    val nom = obj["nombre"].toString()
    val data= obj["data"].toString()
    val objTam = JSONObject(obj["tamaño"].toString())
    val height = objTam["alto"].toString().toInt()
    val wight = objTam["ancho"].toString().toInt()
    var image: BufferedImage? = null
    val imageByte: ByteArray

    val decoder = BASE64Decoder()
    imageByte = decoder.decodeBuffer(data)
    val bis = ByteArrayInputStream(imageByte)
    image = ImageIO.read(bis)
    bis.close()
    if (wight*height>image.width*image.height){
        return "{\n\t\"error\": \"Las dimensiones tienen que ser pequeñas\"\n}"
    }
    var outputfile = File(nom)
    ImageIO.write(image!!, "bmp", outputfile)
    resize(nom,"resize_"+nom,wight,height)

    outputfile = File("resize_"+nom)
    val imageRead = ImageIO.read(outputfile)
    val bos = ByteArrayOutputStream()
    var imageString: String? = null

    try {
        ImageIO.write(imageRead, "bmp", bos)
        val imageBytes = bos.toByteArray()

        val encoder = BASE64Encoder()
        imageString = encoder.encode(imageBytes)

        bos.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val jsonString = "{\n\t\"nombre\": \"resize_${nom}\",\n\t\"data\": \"${imageString}\"\n}"
    return jsonString
}

@Throws(IOException::class)
fun resize(inputImagePath: String,
           outputImagePath: String, scaledWidth: Int, scaledHeight: Int) {
    val inputFile = File(inputImagePath)
    val inputImage = ImageIO.read(inputFile)

    // creates output image
    val outputImage = BufferedImage(scaledWidth,
            scaledHeight, inputImage.type)

    // scales the input image to the output image
    val g2d = outputImage.createGraphics()
    g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null)
    g2d.dispose()

    // extracts extension of output file
    val formatName = outputImagePath.substring(outputImagePath
            .lastIndexOf(".") + 1)

    // writes to output file
    ImageIO.write(outputImage, formatName, File(outputImagePath))
}

fun main(args: Array<String>) {
    val app = Javalin.create().port(8080)
    app.post("/") { req, res-> res.body("index") }

    app.post("/ejercicio1") { req, res-> res.body(ejer1(req.body().toString())) }

    app.post("/ejercicio2") { req, res-> res.body(ejer2(req.body().toString())) }

    app.post("/ejercicio3") { req, res-> res.body(ejer3(req.body().toString())) }

    app.post("/ejercicio4") { req, res-> res.body(ejer4(req.body().toString())) }
}