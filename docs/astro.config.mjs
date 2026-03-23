// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
	site: 'https://gradleup.com',
	base: '/tapmoc',
	integrations: [
		starlight({
			title: 'Tapmoc',
			editLink: {
				baseUrl: 'https://github.com/GradleUp/tapmoc/edit/main/docs/',
			},
			logo: {
				src: './src/assets/logo.svg'
			},
			social: {
				github: 'https://github.com/GradleUp/tapmoc',
			},
			sidebar: [
				{ label: 'Quickstart', link: '/', },
        { label: 'Guidelines for library authors', link: '/guidelines' },
			],
		}),
	],
});
