import animate from "tailwindcss-animate"
import { setupInspiraUI } from "@inspira-ui/plugins"

export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {},
    },
    plugins: [animate, setupInspiraUI],
}