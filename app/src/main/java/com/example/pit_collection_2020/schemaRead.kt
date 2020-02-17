import android.content.Context
import com.example.pit_collection_2020.R

import org.yaml.snakeyaml.Yaml



// Function to read the YAML schema file and put its contents into a dictionary.

fun schemaRead(schemaFile: Int, context: Context): HashMap<String, HashMap<String, Any>> {

    val inputStream = context.getResources().openRawResource(schemaFile)

    val yaml = Yaml()

    val schema: HashMap<String, HashMap<String, Any>> = yaml.load(inputStream)

    return schema

}