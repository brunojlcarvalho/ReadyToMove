package pt.ipp.estg.readytomove.data.model

/**
 * Data class representing a generic row of information containing a label and a value.
 *
 * This is primarily used in UI lists or cards to display key-value pairs of data
 * (e.g., "Email: user@example.com" or "Distance: 5km").
 *
 * @property label The descriptive label of the information.
 * @property value The actual data value to be displayed.
 */
data class InfoRowData(
    val label: String,
    val value: String
)